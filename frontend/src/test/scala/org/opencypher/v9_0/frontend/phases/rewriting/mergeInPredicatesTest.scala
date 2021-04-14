/*
 * Copyright (c) Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_0.frontend.phases.rewriting

import org.opencypher.v9_0.ast.Query
import org.opencypher.v9_0.ast.semantics.SemanticState
import org.opencypher.v9_0.frontend.phases.CNFNormalizer
import org.opencypher.v9_0.rewriting.AstRewritingTestSupport
import org.opencypher.v9_0.rewriting.rewriters.mergeInPredicates
import org.opencypher.v9_0.util.OpenCypherExceptionFactory
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class mergeInPredicatesTest extends CypherFunSuite with AstRewritingTestSupport {

  test("MATCH (a) WHERE a.prop IN [1,2,3] AND a.prop IN [2,3,4] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] AND a.prop IN [2,3,4] RETURN *",
      "MATCH (a) WHERE a.prop IN [2,3] RETURN *")
  }

  test("MATCH (a) WHERE a.prop IN [1,2,3] AND a.prop IN [4,5,6] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] AND a.prop IN [4,5,6] RETURN *",
      "MATCH (a) WHERE false RETURN *")
  }

  test("MATCH (a) WHERE a.prop IN [1,2,3] AND a.prop IN [2,3,4] AND a.prop IN [3,4,5] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] AND a.prop IN [2,3,4] AND a.prop IN [3,4,5] RETURN *",
      "MATCH (a) WHERE a.prop IN [3] RETURN *")
  }

  test("MATCH (a) WHERE (a.prop IN [1,2,3] AND a.prop IN [2,3,4]) OR (a.prop IN [2,3,4] AND a.prop IN [3,4,5]) RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE (a.prop IN [1,2,3] AND a.prop IN [2,3,4]) OR (a.prop IN [2,3,4] AND a.prop IN [3,4,5]) RETURN *",
      "MATCH (a) WHERE a.prop IN [2,3,4] RETURN *")
  }

  test("MATCH (a) WHERE a.prop IN [1,2,3] AND a.foo IN ['foo', 'bar'] AND a.prop IN [2,3,4] AND a.foo IN ['bar'] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] AND a.foo IN ['foo','bar'] AND a.prop IN [2,3,4] AND a.foo IN ['bar'] RETURN *",
      "MATCH (a) WHERE a.prop IN [2,3] AND a.foo IN ['bar'] RETURN *")
  }

  test("MATCH (a) WHERE a.prop IN [1,2,3] OR a.prop IN [2,3,4] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] OR a.prop IN [2,3,4] RETURN *",
      "MATCH (a) WHERE a.prop IN [1,2,3,4] RETURN *")
  }

  test("MATCH (a) WHERE a.prop IN [1,2,3] OR a.prop IN [2,3,4] OR a.prop IN [3,4,5] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] OR a.prop IN [2,3,4] OR a.prop IN [3,4,5] RETURN *",
      "MATCH (a) WHERE a.prop IN [1,2,3,4,5] RETURN *")
  }

  test("MATCH (a) WHERE (a.prop IN [1,2,3] OR a.prop IN [2,3,4]) AND (a.prop IN [2,3,4] OR a.prop IN [3,4,5]) RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE (a.prop IN [1,2,3] OR a.prop IN [2,3,4]) AND (a.prop IN [2,3,4] OR a.prop IN [3,4,5]) RETURN *",
      "MATCH (a) WHERE a.prop IN [2,3,4] RETURN *")
  }

  test("MATCH (a) WHERE a.prop IN [1,2,3] OR a.foo IN ['foo', 'bar'] OR a.prop IN [2,3,4] OR a.foo IN ['bar'] RETURN *") {
    shouldRewrite(
      "MATCH (a) WHERE a.prop IN [1,2,3] OR a.foo IN ['foo','bar'] OR a.prop IN [2,3,4] OR a.foo IN ['bar'] RETURN *",
      "MATCH (a) WHERE a.prop IN [1,2,3,4] OR a.foo IN ['foo','bar'] RETURN *")
  }

  test("MATCH (n) RETURN n.prop IN [1,2,3] AND n.prop IN [3,4,5]") {
    shouldRewrite("MATCH (n) RETURN n.prop IN [1,2,3] AND n.prop IN [3,4,5] AS FOO",
                  "MATCH (n) RETURN n.prop IN [3] AS FOO")
  }

  test("MATCH (n) RETURN (n.prop IN [1,2,3] OR TRUE) AND n.prop IN [3,4,5] AS FOO") {
    shouldNotRewrite("MATCH (n) RETURN (n.prop IN [1,2,3] OR TRUE) AND n.prop IN [3,4,5] AS FOO")
  }

  test("MATCH (n) RETURN (n.prop IN [1,2,3] AND FALSE) OR n.prop IN [3,4,5] AS FOO") {
    shouldNotRewrite("MATCH (n) RETURN (n.prop IN [1,2,3] AND FALSE) OR n.prop IN [3,4,5] AS FOO")
  }

  // Tests for IN combined with NOT

  test( "MATCH (a) WHERE NOT a.prop IN [1, 2, 3] AND NOT a.prop IN [3, 4, 5] RETURN *") {
    shouldRewrite("MATCH (a) WHERE NOT a.prop IN [1, 2, 3] AND NOT a.prop IN [3, 4, 5] RETURN *",
      "MATCH (a) WHERE NOT a.prop IN [1, 2, 3, 4, 5] RETURN *")
  }

  test( "MATCH (a) WHERE NOT a.prop IN [1, 2, 3] OR NOT a.prop IN [3, 4, 5] RETURN *") {
    shouldRewrite("MATCH (a) WHERE NOT a.prop IN [1, 2, 3] OR NOT a.prop IN [3, 4, 5] RETURN *",
      "MATCH (a) WHERE NOT a.prop IN [3] RETURN *")
  }

  test( "MATCH (a) WHERE a.prop IN [1, 2, 3] AND NOT a.prop IN [3, 4, 5]") {
    shouldNotRewrite("MATCH (a) WHERE a.prop IN [1, 2, 3] AND NOT a.prop IN [3, 4, 5]")
  }

  test( "MATCH (a) WHERE NOT a.prop IN [1, 2, 3] OR a.prop IN [3, 4, 5]") {
    shouldNotRewrite("MATCH (a) WHERE a.prop IN [1, 2, 3] OR NOT a.prop IN [3, 4, 5]")
  }

  // would have been nice to rewrite the order of predicates to allow merging in predicates
  test("MATCH (a) WHERE a.prop IN [1,2,3] AND NOT a.prop IN [3,4,5] AND a.prop IN [2,3,4] RETURN *") {
    shouldNotRewrite("MATCH (a) WHERE a.prop IN [1,2,3] AND NOT a.prop IN [3,4,5] AND a.prop IN [2,3,4] RETURN *")
  }

  test("MATCH (a) WHERE NOT (a.prop IN [1,2] AND a.prop IN [2,3])") {
    shouldRewrite("MATCH (a) WHERE NOT (a.prop IN [1,2] AND a.prop IN [2,3])",
      "MATCH (a) WHERE NOT a.prop IN [2]"
    )
  }

  test("MATCH (a) WHERE NOT (a.prop IN [1,2] AND a.prop IN [2,3]) AND NOT (a.prop IN [3,4] AND a.prop IN [4,5])") {
    shouldRewrite("MATCH (a) WHERE NOT (a.prop IN [1,2] AND a.prop IN [2,3]) AND NOT (a.prop IN [3,4] AND a.prop IN [4,5])",
      "MATCH (a) WHERE NOT a.prop IN [2,4]"
    )
  }

  test( "RETURN ANY(i IN [1,2,3]) AND ANY(i IN [4])") {
   shouldNotRewrite("MATCH (n) WHERE ANY(i IN n.prop WHERE i IN [1,2]) AND ANY(i IN n.prop WHERE i IN [3]) RETURN n")
  }

  private def shouldRewrite(from: String, to: String): Unit = {
    val exceptionFactory = OpenCypherExceptionFactory(None)
    val original = parser.parse(from, exceptionFactory).asInstanceOf[Query]
    val expected = parser.parse(to, exceptionFactory).asInstanceOf[Query]
    val checkResults = original.semanticCheck(SemanticState.clean)
    val common = CNFNormalizer.instance(checkResults.state, TestContext())
    val result = mergeInPredicates(original)

    common(result) should equal(common(expected))
  }

  private def shouldNotRewrite(query: String): Unit = shouldRewrite(query, query)

}
