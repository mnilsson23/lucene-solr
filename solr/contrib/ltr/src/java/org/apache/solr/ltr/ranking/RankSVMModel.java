/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.ltr.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.solr.ltr.feature.norm.Normalizer;
import org.apache.solr.ltr.model.LTRScoringModel;
import org.apache.solr.ltr.model.ModelException;

public class RankSVMModel extends LTRScoringModel {

  private static final Double defaultWeight = 0.0;

  protected float[] featureToWeight;

  /** name of the attribute containing the weight of the SVM model **/
  public static final String WEIGHTS_PARAM = "weights";

  public RankSVMModel(String name, List<Feature> features,
      List<Normalizer> norms,
      String featureStoreName, List<Feature> allFeatures,
      Map<String,Object> params) throws ModelException {
    super(name, features, norms, featureStoreName, allFeatures, params);

    final Map<String,Double> modelWeights = (params == null ? null
        : (Map<String,Double>) params.get(WEIGHTS_PARAM));

    featureToWeight = new float[features.size()];

    if (modelWeights != null) {
      for (int i = 0; i < features.size(); ++i) {
        final String key = features.get(i).getName();
        featureToWeight[i] = modelWeights.getOrDefault(key, defaultWeight).floatValue();
      }
    } else {
      for (int i = 0; i < features.size(); ++i) {
        featureToWeight[i] = defaultWeight.floatValue();
      }
    }
  }

  @Override
  public float score(float[] modelFeatureValuesNormalized) {
    float score = 0;
    for (int i = 0; i < modelFeatureValuesNormalized.length; ++i) {
      score += modelFeatureValuesNormalized[i] * featureToWeight[i];
    }
    return score;
  }

  @Override
  public Explanation explain(LeafReaderContext context, int doc,
      float finalScore, List<Explanation> featureExplanations) {
    final List<Explanation> details = new ArrayList<>();
    int index = 0;

    for (final Explanation featureExplain : featureExplanations) {
      final List<Explanation> featureDetails = new ArrayList<>();
      featureDetails.add(Explanation.match(featureToWeight[index],
          "weight on feature"));
      featureDetails.add(featureExplain);

      details.add(Explanation.match(featureExplain.getValue()
          * featureToWeight[index], "prod of:", featureDetails));
      index++;
    }

    return Explanation.match(finalScore, toString()
        + " model applied to features, sum of:", details);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append("(name=").append(getName());
    sb.append(",featureWeights=[");
    for (int ii = 0; ii < features.size(); ++ii) {
      if (ii>0) sb.append(',');
      final String key = features.get(ii).getName();
      sb.append(key).append('=').append(featureToWeight[ii]);
    }
    sb.append("])");
    return sb.toString();
  }

}
