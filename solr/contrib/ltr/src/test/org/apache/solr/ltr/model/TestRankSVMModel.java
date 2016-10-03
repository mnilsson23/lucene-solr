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
package org.apache.solr.ltr.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.ltr.TestRerankBase;
import org.apache.solr.ltr.feature.Feature;
import org.apache.solr.ltr.feature.FeatureStore;
import org.apache.solr.ltr.model.LTRScoringModel;
import org.apache.solr.ltr.model.ModelException;
import org.apache.solr.ltr.model.RankSVMModel;
import org.apache.solr.ltr.norm.IdentityNormalizer;
import org.apache.solr.ltr.norm.Normalizer;
import org.apache.solr.ltr.rest.ManagedModelStore;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRankSVMModel extends TestRerankBase {

  static ManagedModelStore store = null;
  static FeatureStore fstore = null;

  @BeforeClass
  public static void setup() throws Exception {
    setuptest();
    // loadFeatures("features-store-test-model.json");
    store = getNewManagedModelStore();
    fstore = getNewManagedFeatureStore().getFeatureStore("test");

  }

  @Test
  public void getInstanceTest() {
    final Map<String,Object> weights = new HashMap<>();
    weights.put("constant1", 1d);
    weights.put("constant5", 1d);

    Map<String,Object> params = new HashMap<String,Object>();
    final List<Feature> features = getFeatures(new String[] {
        "constant1", "constant5"});
    final List<Normalizer> norms = 
        new ArrayList<Normalizer>(
            Collections.nCopies(features.size(),IdentityNormalizer.INSTANCE));
    params.put("weights", weights);
    final LTRScoringModel meta = RankSVMModel.create("test1",
        features, norms, "test", fstore.getFeatures(),
        params);

    store.addModel(meta);
    final LTRScoringModel m = store.getModel("test1");
    assertEquals(meta, m);
  }

  @Test
  public void nullFeatureWeightsTest() {
    final ModelException expectedException = 
        new ModelException("Model test2 doesn't contain any weights");
    try {
      final List<Feature> features = getFeatures(new String[] 
          {"constant1", "constant5"});
      final List<Normalizer> norms = 
        new ArrayList<Normalizer>(
            Collections.nCopies(features.size(),IdentityNormalizer.INSTANCE));
      final LTRScoringModel meta = RankSVMModel.create("test2",
          features, norms, "test", fstore.getFeatures(), null);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }

  @Test
  public void existingNameTest() {
    final SolrException expectedException = 
        new SolrException(ErrorCode.BAD_REQUEST,
            ModelException.class.getCanonicalName()+": model 'test3' already exists. Please use a different name");
    try {
      final List<Feature> features = getFeatures(new String[] 
          {"constant1", "constant5"});
      final List<Normalizer> norms = 
        new ArrayList<Normalizer>(
            Collections.nCopies(features.size(),IdentityNormalizer.INSTANCE));
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringModel meta = RankSVMModel.create("test3",
          features, norms, "test", fstore.getFeatures(),
              params);
      store.addModel(meta);
      final LTRScoringModel m = store.getModel("test3");
      assertEquals(meta, m);
      store.addModel(meta);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (SolrException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }

  @Test
  public void duplicateFeatureTest() {
    final SolrException expectedException = 
        new SolrException(ErrorCode.BAD_REQUEST,
            ModelException.class.getCanonicalName()+": duplicated feature constant1 in model test4");
    try {
      final List<Feature> features = getFeatures(new String[] 
          {"constant1", "constant1"});
      final List<Normalizer> norms = 
        new ArrayList<Normalizer>(
            Collections.nCopies(features.size(),IdentityNormalizer.INSTANCE));
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringModel meta = RankSVMModel.create("test4",
          features, norms, "test", fstore.getFeatures(),
              params);
      store.addModel(meta);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (SolrException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }

  }

  @Test
  public void missingFeatureWeightTest() {
    final ModelException expectedException = 
        new ModelException("Model test5 lacks weight(s) for [constant5]");
    try {
      final List<Feature> features = getFeatures(new String[] 
          {"constant1", "constant5"});
      final List<Normalizer> norms = 
        new ArrayList<Normalizer>(
            Collections.nCopies(features.size(),IdentityNormalizer.INSTANCE));
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5missing", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringModel meta = RankSVMModel.create("test5",
          features, norms, "test", fstore.getFeatures(),
              params);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (ModelException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }

  @Test
  public void emptyFeaturesTest() {
    final SolrException expectedException = 
        new SolrException(ErrorCode.BAD_REQUEST,
            ModelException.class.getCanonicalName()+": no features declared for model test6");
    try {
      final List<Feature> features = getFeatures(new String[] {});
      final List<Normalizer> norms = 
        new ArrayList<Normalizer>(
            Collections.nCopies(features.size(),IdentityNormalizer.INSTANCE));
      final Map<String,Object> weights = new HashMap<>();
      weights.put("constant1", 1d);
      weights.put("constant5missing", 1d);

      Map<String,Object> params = new HashMap<String,Object>();
      params.put("weights", weights);
      final LTRScoringModel meta = RankSVMModel.create("test6",
          features, norms, "test", fstore.getFeatures(),
          params);
      store.addModel(meta);
      fail("unexpectedly got here instead of catching "+expectedException);
    } catch (SolrException actualException) {
      assertEquals(expectedException.toString(), actualException.toString());
    }
  }
}