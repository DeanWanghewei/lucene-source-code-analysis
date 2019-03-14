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
package lucene.search.similarities;


import lucene.search.Explanation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Language model based on the Jelinek-Mercer smoothing method. From Chengxiang
 * Zhai and John Lafferty. 2001. A study of smoothing methods for language
 * models applied to Ad Hoc information retrieval. In Proceedings of the 24th
 * annual international ACM SIGIR conference on Research and development in
 * information retrieval (SIGIR '01). ACM, New York, NY, USA, 334-342.
 * <p>The model has a single parameter, &lambda;. According to said paper, the
 * optimal value depends on both the collection and the query. The optimal value
 * is around {@code 0.1} for title queries and {@code 0.7} for long queries.</p>
 * <p>Values should be between 0 (exclusive) and 1 (inclusive). Values near zero act score more
 * like a conjunction (coordinate level matching), whereas values near 1 behave
 * the opposite (more like pure disjunction).
 * @lucene.experimental
 */
public class LMJelinekMercerSimilarity extends LMSimilarity {
  /** The &lambda; parameter. */
  private final float lambda;
  
  /** Instantiates with the specified collectionModel and &lambda; parameter. */
  public LMJelinekMercerSimilarity(
      CollectionModel collectionModel, float lambda) {
    super(collectionModel);
    if (Float.isNaN(lambda) || lambda <= 0 || lambda > 1) {
      throw new IllegalArgumentException("lambda must be in the range (0 .. 1]");
    }
    this.lambda = lambda;
  }

  /** Instantiates with the specified &lambda; parameter. */
  public LMJelinekMercerSimilarity(float lambda) {
    if (Float.isNaN(lambda) || lambda <= 0 || lambda > 1) {
      throw new IllegalArgumentException("lambda must be in the range (0 .. 1]");
    }
    this.lambda = lambda;
  }
  
  @Override
  protected double score(BasicStats stats, double freq, double docLen) {
    return stats.getBoost() *
            Math.log(1 +
            ((1 - lambda) * freq / docLen) /
            (lambda * ((LMStats)stats).getCollectionProbability()));
  }

  @Override
  protected void explain(List<Explanation> subs, BasicStats stats,
                         double freq, double docLen) {
    if (stats.getBoost() != 1.0d) {
      subs.add(Explanation.match((float) stats.getBoost(), "boost"));
    }
    subs.add(Explanation.match(lambda, "lambda"));
    double p = ((LMStats)stats).getCollectionProbability();
    Explanation explP = Explanation.match((float) p,
        "P, probability that the current term is generated by the collection");
    subs.add(explP);
    Explanation explFreq = Explanation.match((float) freq,
        "freq, number of occurrences of term in the document");
    subs.add(explFreq);
    subs.add(Explanation.match((float) docLen,"dl, length of field"));
    super.explain(subs, stats, freq, docLen);
  }

  @Override
  protected Explanation explain(
          BasicStats stats, Explanation freq, double docLen) {
    List<Explanation> subs = new ArrayList<>();
    explain(subs, stats, freq.getValue().doubleValue(), docLen);

    return Explanation.match(
        (float) score(stats, freq.getValue().doubleValue(), docLen),
        "score(" + getClass().getSimpleName() + ", freq=" +
            freq.getValue() +"), computed as boost * " +
            "log(1 + ((1 - lambda) * freq / dl) /(lambda * P)) from:",
        subs);
  }

  /** Returns the &lambda; parameter. */
  public float getLambda() {
    return lambda;
  }

  @Override
  public String getName() {
    return String.format(Locale.ROOT, "Jelinek-Mercer(%f)", getLambda());
  }
}
