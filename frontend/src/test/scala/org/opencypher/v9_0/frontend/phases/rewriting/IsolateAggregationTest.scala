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

import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.ast.Statement
import org.opencypher.v9_0.frontend.phases.Monitors
import org.opencypher.v9_0.frontend.phases.isolateAggregation
import org.opencypher.v9_0.frontend.phases.rewriting.cnf.TestContext
import org.opencypher.v9_0.rewriting.RewriteTest
import org.opencypher.v9_0.rewriting.rewriters.normalizeWithAndReturnClauses
import org.opencypher.v9_0.util.AllNameGenerators
import org.opencypher.v9_0.util.OpenCypherExceptionFactory
import org.opencypher.v9_0.util.Rewriter
import org.opencypher.v9_0.util.devNullLogger
import org.opencypher.v9_0.util.inSequence
import org.opencypher.v9_0.util.test_helpers.CypherFunSuite

class IsolateAggregationTest extends CypherFunSuite with RewriteTest with AstConstructionTestSupport {
  def rewriterUnderTest: Rewriter = isolateAggregation.instance(new TestContext(mock[Monitors]) {
    override val allNameGenerators: AllNameGenerators = new AllNameGenerators
  })

  test("refers to renamed variable in where clause") {
    assertRewrite(
      """
        |MATCH (owner)
        |WITH owner, count(*) > 0 AS collected
        |WHERE (owner)-->()
        |RETURN owner
      """.stripMargin,
      """
        |MATCH (owner)
        |WITH owner AS `  AGGREGATION0`, count(*) AS `  AGGREGATION1`
        |WITH `  AGGREGATION0` AS owner, `  AGGREGATION1` > 0 AS collected
        |  WHERE (owner)-->()
        |RETURN owner AS owner
      """.stripMargin)
  }

  test("refers to renamed variable in order by clause") {
    assertRewrite(
      """
        |MATCH (owner)
        |WITH owner, count(*) > 0 AS collected
        |ORDER BY owner.foo
        |RETURN owner
      """.stripMargin,
      """
        |MATCH (owner)
        |WITH owner AS `  AGGREGATION0`, count(*) AS `  AGGREGATION1`
        |WITH `  AGGREGATION0` AS owner, `  AGGREGATION1` > 0 AS collected
        |  ORDER BY owner.foo
        |RETURN owner AS owner
      """.stripMargin)
  }

  test("does not rewrite things that should not be rewritten") {
    assertIsNotRewritten("MATCH n RETURN n AS n")
    assertIsNotRewritten("MATCH n RETURN n AS n, count(*) AS count, max(n.prop) AS max")
  }

  test("MATCH (n) RETURN { name: n.name, count: count(*) } AS result") {
    assertRewrite(
      "MATCH (n) RETURN { name: n.name, count: count(*) } AS result",
      "MATCH (n) WITH n.name AS `  AGGREGATION0`, count(*) AS `  AGGREGATION1` RETURN { name: `  AGGREGATION0`, count: `  AGGREGATION1` } AS result")
  }

  test("MATCH (n) RETURN n.foo + count(*) AS result") {
    assertRewrite(
      "MATCH (n) RETURN n.foo + count(*) AS result",
      "MATCH (n) WITH n.foo AS `  AGGREGATION0`, count(*) AS `  AGGREGATION1` RETURN `  AGGREGATION0` + `  AGGREGATION1` AS result")
  }

  test("MATCH (n) RETURN count(*)/60/42 AS result") {
    assertRewrite(
      "MATCH (n) RETURN count(*)/60/42 AS result",
      "MATCH (n) WITH count(*) AS `  AGGREGATION0` RETURN `  AGGREGATION0`/60/42 AS result")
  }

  test("WITH 60 as sixty, 42 as fortytwo MATCH (n) RETURN count(*)/sixty/fortytwo AS result") {
    assertRewrite(
      "WITH 60 as sixty, 42 as fortytwo MATCH (n) RETURN count(*)/sixty/fortytwo AS result",
      "WITH 60 as sixty, 42 as fortytwo MATCH (n) WITH count(*) AS `  AGGREGATION0`, sixty AS `  AGGREGATION1`, fortytwo AS `  AGGREGATION2` RETURN `  AGGREGATION0`/`  AGGREGATION1`/`  AGGREGATION2` AS result")
  }

  test("WITH 1 AS node, [] AS nodes1 RETURN ANY (n IN collect(distinct node) WHERE n IN nodes1) as count") {
    assertRewrite(
      """WITH 1 AS node, [] AS nodes1
        |RETURN ANY (n IN collect(distinct node) WHERE n IN nodes1) as count""".stripMargin,
      """WITH 1 AS node, [] AS nodes1
        |WITH collect(distinct node) AS `  AGGREGATION0`, nodes1 AS `  AGGREGATION1`
        |RETURN ANY (n IN `  AGGREGATION0` WHERE n IN `  AGGREGATION1`) as count""".stripMargin)
  }

  test("WITH 1 AS node, [] AS nodes1 RETURN NONE(n IN collect(distinct node) WHERE n IN nodes1) as count") {
    assertRewrite(
      """WITH 1 AS node, [] AS nodes1
        |RETURN NONE(n IN collect(distinct node) WHERE n IN nodes1) as count""".stripMargin,
      """WITH 1 AS node, [] AS nodes1
        |WITH collect(distinct node) AS `  AGGREGATION0`, nodes1 AS `  AGGREGATION1`
        |RETURN NONE(n IN `  AGGREGATION0` WHERE n IN `  AGGREGATION1`) as count""".stripMargin)
  }

  test("MATCH (n)-->() RETURN (n)-->({k: count(*)}) AS result") {
    assertRewrite(
      "MATCH (n)-->() RETURN (n)-->({k: count(*)}) AS result",
      "MATCH (n)-->() WITH n as `  AGGREGATION0`, count(*) AS `  AGGREGATION1` RETURN (`  AGGREGATION0`)-->({k:`  AGGREGATION1`}) AS result")
  }

  test("MATCH (n) RETURN n.prop AS prop, n.foo + count(*) AS count") {
    assertRewrite(
      "MATCH (n) RETURN n.prop AS prop, n.foo + count(*) AS count",
      "MATCH (n) WITH n.prop AS `  AGGREGATION0`, n.foo AS `  AGGREGATION1`, count(*) AS `  AGGREGATION2` RETURN `  AGGREGATION0` AS prop, `  AGGREGATION1` + `  AGGREGATION2` AS count")
  }

  test("MATCH (n) RETURN n AS n, count(n) + 3 AS count") {
    assertRewrite(
      "MATCH (n) RETURN n AS n, count(n) + 3 AS count",
      "MATCH (n) WITH n AS `  AGGREGATION0`, count(n) as `  AGGREGATION1`  RETURN `  AGGREGATION0` AS n, `  AGGREGATION1` + 3 AS count")
  }

  test("UNWIND [1,2,3] AS a RETURN reduce(y=0, x IN collect(a) | x) AS z") {
    assertRewrite(
      "UNWIND [1,2,3] AS a RETURN reduce(y=0, x IN collect(a) | x) AS z",
      "UNWIND [1,2,3] AS a WITH collect(a) AS `  AGGREGATION0` RETURN reduce(y=0, x IN `  AGGREGATION0` | x) AS z")
  }

  test("UNWIND [1,2,3] AS a RETURN [x IN collect(a) | x] AS z") {
    assertRewrite(
      "UNWIND [1,2,3] AS a RETURN [x IN collect(a) | x] AS z",
      "UNWIND [1,2,3] AS a WITH collect(a) AS `  AGGREGATION0` RETURN [x IN `  AGGREGATION0` | x] AS z")
  }

  test("MATCH (n) WITH 60/60/count(*) AS x RETURN x AS x") {
    assertRewrite(
      "MATCH (n) WITH 60/60/count(*) AS x RETURN x AS x",
      "MATCH (n) WITH count(*) AS `  AGGREGATION0` WITH 60/60/`  AGGREGATION0` AS x RETURN x AS x")
  }

  test("MATCH (a:Start)<-[:R]-(b) RETURN { foo:a.prop=42, bar:collect(b.prop2) } AS result") {
    assertRewrite(
      "MATCH (a:Start)<-[:R]-(b) " +
        "RETURN { foo:a.prop=42, bar:collect(b.prop2) } AS result",

      "MATCH (a:Start)<-[:R]-(b) " +
        "WITH a.prop=42 AS `  AGGREGATION0`, collect(b.prop2) AS `  AGGREGATION1` " +
        "RETURN { foo:`  AGGREGATION0`, bar:`  AGGREGATION1`} AS result")
  }

  test("MATCH (n) RETURN count(*) + max(id(n)) AS r") {
    assertRewrite(
      "MATCH (n) RETURN count(*) + max(id(n)) AS r",
      "MATCH (n) WITH count(*) AS `  AGGREGATION0`, max(id(n)) AS `  AGGREGATION1` RETURN `  AGGREGATION0`+`  AGGREGATION1` AS r")
  }

  test("MATCH (a) RETURN size(collect(a)) AS size") {
    assertRewrite(
      "MATCH (a) RETURN size(collect(a)) AS size",
      "MATCH (a) WITH collect(a) AS `  AGGREGATION0` RETURN size(`  AGGREGATION0`) AS size")
  }

  test("MATCH (a) RETURN count(a) > 0 AS bool") {
    assertRewrite(
      "MATCH (a) RETURN count(a) > 0 AS bool",
      "MATCH (a) WITH count(a) AS `  AGGREGATION0` RETURN `  AGGREGATION0` > 0 AS bool")
  }

  test("MATCH (a) RETURN count(a) > $param AS bool") {
    assertRewrite(
      "MATCH (a) RETURN count(a) > $param AS bool",
      "MATCH (a) WITH count(a) AS `  AGGREGATION0` RETURN `  AGGREGATION0` > $param AS bool")
  }

  test("should not introduce multiple return items for the same expression") {
    assertRewrite(
      "WITH 1 AS x, 2 AS y RETURN sum(x)*y AS a, sum(x)*y AS b",
      "WITH 1 AS x, 2 AS y WITH sum(x) as `  AGGREGATION0`, y as `  AGGREGATION1` RETURN `  AGGREGATION0`* `  AGGREGATION1` AS a, `  AGGREGATION0`*`  AGGREGATION1` AS b")
  }

  test("MATCH (a), (b) RETURN coalesce(a.prop, b.prop), b.prop, { x: count(b) }") {
    assertRewrite(
      """MATCH (a), (b)
        |RETURN coalesce(a.prop, b.prop), b.prop, { x: count(b) }""".stripMargin,
      """MATCH (a), (b)
        |WITH coalesce(a.prop, b.prop) AS `  AGGREGATION0`,
        |     b.prop AS `  AGGREGATION1`,
        |     count(b) AS `  AGGREGATION2`
        |RETURN `  AGGREGATION0` AS `coalesce(a.prop, b.prop)`,
        |       `  AGGREGATION1` AS `b.prop`,
        |       { x: `  AGGREGATION2` } AS `{ x: count(b) }`""".stripMargin)
  }

  test("should not extract expressions that do not contain on variables as implicit grouping key") {
    assertRewrite(
      """MATCH (user:User {userId: 11})-[friendship:FRIEND]-()
        |WITH user AS user, collect(friendship)[toInteger(rand() * count(friendship))] AS selectedFriendship
        |RETURN id(selectedFriendship) AS friendshipId, selectedFriendship.propFive AS propertyValue""".stripMargin,
      """MATCH (user:User {userId: 11})-[friendship:FRIEND]-()
        |WITH user AS `  AGGREGATION0`, collect(friendship) AS `  AGGREGATION1`, count(friendship) AS `  AGGREGATION2`
        |WITH `  AGGREGATION0` AS user, `  AGGREGATION1`[toInteger(rand() * `  AGGREGATION2`)] AS selectedFriendship
        |RETURN id(selectedFriendship) AS friendshipId, selectedFriendship.propFive AS propertyValue""".stripMargin
    )
  }

  override protected def parseForRewriting(queryText: String): Statement = {
    val exceptionFactory = OpenCypherExceptionFactory(Some(pos))
    super.parseForRewriting(queryText).endoRewrite(inSequence(normalizeWithAndReturnClauses(exceptionFactory, devNullLogger)))
  }
}
