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
package org.opencypher.v9_0.parser

import org.opencypher.v9_0.ast
import org.opencypher.v9_0.ast.AstConstructionTestSupport
import org.opencypher.v9_0.ast.IfExistsDoNothing
import org.opencypher.v9_0.ast.IfExistsInvalidSyntax
import org.opencypher.v9_0.ast.IfExistsReplace
import org.opencypher.v9_0.ast.IfExistsThrowError
import org.opencypher.v9_0.expressions
import org.opencypher.v9_0.util.InputPosition
import org.parboiled.scala.Rule1

class SchemaCommandParserTest
  extends ParserAstTest[ast.SchemaCommand]
    with SchemaCommand
    with AstConstructionTestSupport {

  implicit val parser: Rule1[ast.SchemaCommand] = SchemaCommand

  // Create node index (old syntax)

  test("CREATE INDEX ON :Person(name)") {
    yields(ast.CreateIndexOldSyntax(labelName("Person"), List(propertyKeyName("name"))))
  }

  test("CREATE INDEX ON :Person(name,age)") {
    yields(ast.CreateIndexOldSyntax(labelName("Person"), List(propertyKeyName("name"), propertyKeyName("age"))))
  }

  test("CREATE INDEX my_index ON :Person(name)") {
    failsToParse
  }

  test("CREATE INDEX my_index ON :Person(name,age)") {
    failsToParse
  }

  // Create index

  type CreateIndexFunction = (List[expressions.Property], Option[String], ast.IfExistsDo, Map[String, expressions.Expression]) => InputPosition => ast.CreateIndex

  private def nodeIndex(props: List[expressions.Property],
                        name: Option[String],
                        ifExistsDo: ast.IfExistsDo,
                        options: Map[String, expressions.Expression]): InputPosition => ast.CreateIndex =
    ast.CreateNodeIndex(varFor("n"), labelName("Person"), props, name, ifExistsDo, options)

  private def relIndex(props: List[expressions.Property],
                        name: Option[String],
                        ifExistsDo: ast.IfExistsDo,
                        options: Map[String, expressions.Expression]): InputPosition => ast.CreateIndex =
    ast.CreateRelationshipIndex(varFor("n"), relTypeName("R"), props, name, ifExistsDo, options)

  Seq(
    ("(n:Person)", nodeIndex: CreateIndexFunction),
    ("()-[n:R]-()", relIndex: CreateIndexFunction),
    ("()-[n:R]->()", relIndex: CreateIndexFunction),
    ("()<-[n:R]-()", relIndex: CreateIndexFunction)
  ).foreach {
    case (pattern, createIndex: CreateIndexFunction) =>
      test(s"CREATE INDEX FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsThrowError, Map.empty))
      }

      test(s"USE neo4j CREATE INDEX FOR $pattern ON (n.name)") {
        yields(_ => createIndex(List(prop("n", "name")), None, IfExistsThrowError, Map.empty).withGraph(Some(use(varFor("neo4j")))))
      }

      test(s"CREATE INDEX FOR $pattern ON (n.name, n.age)") {
        yields(createIndex(List(prop("n", "name"), prop("n", "age")), None, IfExistsThrowError, Map.empty))
      }

      test(s"CREATE BTREE INDEX my_index FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), Some("my_index"), IfExistsThrowError, Map.empty))
      }

      test(s"CREATE INDEX my_index FOR $pattern ON (n.name, n.age)") {
        yields(createIndex(List(prop("n", "name"), prop("n", "age")), Some("my_index"), IfExistsThrowError, Map.empty))
      }

      test(s"CREATE INDEX `$$my_index` FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), Some("$my_index"), IfExistsThrowError, Map.empty))
      }

      test(s"CREATE OR REPLACE INDEX FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsReplace, Map.empty))
      }

      test(s"CREATE OR REPLACE INDEX my_index FOR $pattern ON (n.name, n.age)") {
        yields(createIndex(List(prop("n", "name"), prop("n", "age")), Some("my_index"), IfExistsReplace, Map.empty))
      }

      test(s"CREATE OR REPLACE INDEX IF NOT EXISTS FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsInvalidSyntax, Map.empty))
      }

      test(s"CREATE OR REPLACE INDEX my_index IF NOT EXISTS FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), Some("my_index"), IfExistsInvalidSyntax, Map.empty))
      }

      test(s"CREATE INDEX IF NOT EXISTS FOR $pattern ON (n.name, n.age)") {
        yields(createIndex(List(prop("n", "name"), prop("n", "age")), None, IfExistsDoNothing, Map.empty))
      }

      test(s"CREATE INDEX my_index IF NOT EXISTS FOR $pattern ON (n.name)") {
        yields(createIndex(List(prop("n", "name")), Some("my_index"), IfExistsDoNothing, Map.empty))
      }

      test(s"CREATE INDEX FOR $pattern ON (n.name) OPTIONS {indexProvider : 'native-btree-1.0'}") {
        yields(createIndex(List(prop("n", "name")),
          None, IfExistsThrowError, Map("indexProvider" -> literalString("native-btree-1.0"))))
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n.name) OPTIONS {indexProvider : 'lucene+native-3.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsThrowError,
          Map("indexProvider" -> literalString("lucene+native-3.0"),
            "indexConfig"   -> mapOf(
              "spatial.cartesian.max" -> listOf(literalFloat(100.0), literalFloat(100.0)),
              "spatial.cartesian.min" -> listOf(literalFloat(-100.0), literalFloat(-100.0))
            )
          )
        ))
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n.name) OPTIONS {indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }, indexProvider : 'lucene+native-3.0'}") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsThrowError,
          Map("indexProvider" -> literalString("lucene+native-3.0"),
            "indexConfig"   -> mapOf(
              "spatial.cartesian.max" -> listOf(literalFloat(100.0), literalFloat(100.0)),
              "spatial.cartesian.min" -> listOf(literalFloat(-100.0), literalFloat(-100.0))
            )
          )
        ))
      }

      test(s"CREATE BTREE INDEX FOR $pattern ON (n.name) OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsThrowError,
          Map("indexConfig" -> mapOf(
            "spatial.wgs-84.max" -> listOf(literalFloat(60.0), literalFloat(60.0)),
            "spatial.wgs-84.min" -> listOf(literalFloat(-40.0), literalFloat(-40.0))
          ))
        ))
      }

      test(s"CREATE INDEX FOR $pattern ON (n.name) OPTIONS {nonValidOption : 42}") {
        yields(createIndex(List(prop("n", "name")), None, IfExistsThrowError, Map("nonValidOption" -> literalInt(42))))
      }

      test(s"CREATE INDEX my_index FOR $pattern ON (n.name) OPTIONS {}") {
        yields(createIndex(List(prop("n", "name")), Some("my_index"), IfExistsThrowError, Map.empty))
      }

      test(s"CREATE INDEX $$my_index FOR $pattern ON (n.name)") {
        failsToParse
      }

      test(s"CREATE INDEX FOR $pattern ON n.name") {
        failsToParse
      }

      test(s"CREATE INDEX FOR $pattern ON (n.name) {indexProvider : 'native-btree-1.0'}") {
        failsToParse
      }

      test(s"CREATE INDEX FOR $pattern ON (n.name) OPTIONS") {
        failsToParse
      }
  }

  test("CREATE INDEX FOR n:Person ON (n.name)") {
    failsToParse
  }

  test("CREATE INDEX FOR -[n:R]-() ON (n.name)") {
    failsToParse
  }

  test("CREATE INDEX FOR ()-[n:R]- ON (n.name)") {
    failsToParse
  }

  test("CREATE INDEX FOR -[n:R]- ON (n.name)") {
    failsToParse
  }

  test("CREATE INDEX FOR [n:R] ON (n.name)") {
    failsToParse
  }

  // Drop index

  test("DROP INDEX ON :Person(name)") {
    yields(ast.DropIndex(labelName("Person"), List(propertyKeyName("name"))))
  }

  test("DROP INDEX ON :Person(name, age)") {
    yields(ast.DropIndex(labelName("Person"), List(propertyKeyName("name"), propertyKeyName("age"))))
  }

  test("DROP INDEX my_index") {
    yields(ast.DropIndexOnName("my_index", ifExists = false))
  }

  test("DROP INDEX `$my_index`") {
    yields(ast.DropIndexOnName("$my_index", ifExists = false))
  }

  test("DROP INDEX my_index IF EXISTS") {
    yields(ast.DropIndexOnName("my_index", ifExists = true))
  }

  test("DROP INDEX $my_index") {
    failsToParse
  }

  test("DROP INDEX my_index ON :Person(name)") {
    failsToParse
  }

  test("DROP INDEX ON (:Person(name))") {
    failsToParse
  }

  test("DROP INDEX ON (:Person {name})") {
    failsToParse
  }

  test("DROP INDEX ON [:Person(name)]") {
    failsToParse
  }

  test("DROP INDEX ON -[:Person(name)]-") {
    failsToParse
  }

  test("DROP INDEX ON ()-[:Person(name)]-()") {
    failsToParse
  }

  test("DROP INDEX ON [:Person {name}]") {
    failsToParse
  }

  test("DROP INDEX ON -[:Person {name}]-") {
    failsToParse
  }

  test("DROP INDEX ON ()-[:Person {name}]-()") {
    failsToParse
  }

  // Create constraint

  // Without name

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsReplace, Map.empty))
  }

  test("CREATE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT (node.prop) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsDoNothing, Map.empty))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), None, IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), None, IfExistsInvalidSyntax, Map.empty))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS {indexProvider : 'native-btree-1.0'}") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")),
      None, IfExistsThrowError, Map("indexProvider" -> literalString("native-btree-1.0"))))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS {indexProvider : 'lucene+native-3.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError,
      Map("indexProvider" -> literalString("lucene+native-3.0"),
          "indexConfig"   -> mapOf(
            "spatial.cartesian.max" -> listOf(literalFloat(100.0), literalFloat(100.0)),
            "spatial.cartesian.min" -> listOf(literalFloat(-100.0), literalFloat(-100.0))
          )
      )
    ))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError,
      Map("indexConfig" -> mapOf(
        "spatial.wgs-84.max" -> listOf(literalFloat(60.0), literalFloat(60.0)),
        "spatial.wgs-84.min" -> listOf(literalFloat(-40.0), literalFloat(-40.0))
      ))
    ))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS {nonValidOption : 42}") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError, Map("nonValidOption" -> literalInt(42))))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS {}") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError, Map.empty))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT node.prop IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT ON (node:Label) ASSERT node.prop IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsReplace, Map.empty))
  }

  test("CREATE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT node.prop IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsDoNothing, Map.empty))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError, Map.empty))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), None, IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), None, IfExistsInvalidSyntax, Map.empty))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS UNIQUE OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0]}, indexProvider : 'native-btree-1.0'}") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), None, IfExistsThrowError,
      Map("indexProvider" -> literalString("native-btree-1.0"),
          "indexConfig"   -> mapOf(
            "spatial.wgs-84.max" -> listOf(literalFloat(60.0), literalFloat(60.0)),
            "spatial.wgs-84.min" -> listOf(literalFloat(-40.0), literalFloat(-40.0))
          )
      )
    ))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsThrowError, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsReplace, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsInvalidSyntax, oldSyntax = true))
  }

  test("CREATE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsDoNothing, oldSyntax = true))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT EXISTS (node.prop) OPTIONS {}") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsThrowError, oldSyntax = true, Some(Map.empty)))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT node.prop IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT ON (node:Label) ASSERT node.prop IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsReplace, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT node.prop IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsInvalidSyntax, oldSyntax = false))
  }

  test("CREATE CONSTRAINT IF NOT EXISTS ON (node:Label) ASSERT (node.prop) IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), None, IfExistsDoNothing, oldSyntax = false))
  }

  test("CREATE CONSTRAINT ON ()-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = true))
  }

  test("CREATE CONSTRAINT ON ()-[r:R]->() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = true))
  }

  test("CREATE CONSTRAINT ON ()<-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT ON ()<-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsReplace, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT IF NOT EXISTS ON ()-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsInvalidSyntax, oldSyntax = true))
  }

  test("CREATE CONSTRAINT IF NOT EXISTS ON ()-[r:R]->() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsDoNothing, oldSyntax = true))
  }

  test("CREATE CONSTRAINT ON ()-[r:R]-() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE CONSTRAINT ON ()-[r:R]->() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE CONSTRAINT ON ()<-[r:R]-() ASSERT (r.prop) IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT ON ()<-[r:R]-() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsReplace, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT IF NOT EXISTS ON ()-[r:R]-() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsInvalidSyntax, oldSyntax = false))
  }

  test("CREATE CONSTRAINT IF NOT EXISTS ON ()-[r:R]->() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsDoNothing, oldSyntax = false))
  }

  test("CREATE CONSTRAINT ON ()-[r:R]-() ASSERT (r.prop) IS NOT NULL OPTIONS {}") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), None, IfExistsThrowError, oldSyntax = false, Some(Map.empty)))
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT node.prop IS NODE KEY") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY {indexProvider : 'native-btree-1.0'}") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT node.prop.part IS UNIQUE") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop.part) IS UNIQUE") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop) IS UNIQUE {indexProvider : 'native-btree-1.0'}") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT (node.prop1, node.prop2) IS UNIQUE OPTIONS") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON (node:Label) ASSERT EXISTS node.prop") {
    failsToParse
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT EXISTS (node.prop) IS NOT NULL") {
    failsToParse
  }

  test("CREATE CONSTRAINT ON ()-[r:R]-() ASSERT EXISTS r.prop") {
    failsToParse
  }

  test("CREATE CONSTRAINT my_constraint ON ()-[r:R]-() ASSERT r.prop IS NULL") {
    failsToParse
  }

  // With name

  test("USE neo4j CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsThrowError, Map.empty, Some(use(varFor("neo4j")))))
  }

  test("USE neo4j CREATE OR REPLACE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT (node.prop) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsInvalidSyntax, Map.empty, Some(use(varFor("neo4j")))))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), Some("my_constraint"), IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), Some("my_constraint"), IfExistsReplace, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), Some("my_constraint"), IfExistsDoNothing, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS NODE KEY OPTIONS {indexProvider : 'native-btree-1.0', indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0]}}") {
    yields(ast.CreateNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsThrowError,
      Map("indexProvider" -> literalString("native-btree-1.0"),
          "indexConfig"   -> mapOf(
            "spatial.wgs-84.max" -> listOf(literalFloat(60.0), literalFloat(60.0)),
            "spatial.wgs-84.min" -> listOf(literalFloat(-40.0), literalFloat(-40.0))
          )
      )
    ))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT node.prop IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsThrowError, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT (node.prop) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsInvalidSyntax, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), Some("my_constraint"), IfExistsThrowError, Map.empty))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), Some("my_constraint"), IfExistsReplace, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2")), Some("my_constraint"), IfExistsDoNothing, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS UNIQUE OPTIONS {indexProvider : 'native-btree-1.0'}") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")),
      Some("my_constraint"), IfExistsThrowError, Map("indexProvider" -> literalString("native-btree-1.0"))))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS UNIQUE OPTIONS {indexProvider : 'lucene+native-3.0', indexConfig : {`spatial.cartesian.max`: [100.0,100.0], `spatial.cartesian.min`: [-100.0,-100.0] }}") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsThrowError,
      Map("indexProvider" -> literalString("lucene+native-3.0"),
          "indexConfig"   -> mapOf(
            "spatial.cartesian.max" -> listOf(literalFloat(100.0), literalFloat(100.0)),
            "spatial.cartesian.min" -> listOf(literalFloat(-100.0), literalFloat(-100.0))
          )
      )
    ))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS UNIQUE OPTIONS {indexConfig : {`spatial.wgs-84.max`: [60.0,60.0], `spatial.wgs-84.min`: [-40.0,-40.0] }}") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")), Some("my_constraint"), IfExistsThrowError,
      Map("indexConfig" -> mapOf(
        "spatial.wgs-84.max" -> listOf(literalFloat(60.0), literalFloat(60.0)),
        "spatial.wgs-84.min" -> listOf(literalFloat(-40.0), literalFloat(-40.0))
      ))
    ))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS UNIQUE OPTIONS {nonValidOption : 42}") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop")),
      Some("my_constraint"), IfExistsThrowError, Map("nonValidOption" -> literalInt(42))))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE OPTIONS {}") {
    yields(ast.CreateUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop1"), prop("node", "prop2")),
      Some("my_constraint"), IfExistsThrowError, Map.empty))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsThrowError, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsReplace, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsInvalidSyntax, oldSyntax = true))
  }

  test("CREATE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsDoNothing, oldSyntax = true))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop) IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint ON (node:Label) ASSERT node.prop IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsReplace, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT node.prop IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsInvalidSyntax, oldSyntax = false))
  }

  test("CREATE CONSTRAINT my_constraint IF NOT EXISTS ON (node:Label) ASSERT node.prop IS NOT NULL") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsDoNothing, oldSyntax = false))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT node.prop IS NOT NULL OPTIONS {}") {
    yields(ast.CreateNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop"), Some("my_constraint"), IfExistsThrowError, oldSyntax = false, Some(Map.empty)))
  }

  test("CREATE CONSTRAINT `$my_constraint` ON ()-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsThrowError, oldSyntax = true))
  }

  test("CREATE CONSTRAINT my_constraint ON ()-[r:R]-() ASSERT EXISTS (r.prop) OPTIONS {}") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("my_constraint"), IfExistsThrowError, oldSyntax = true, Some(Map.empty)))
  }

  test("CREATE OR REPLACE CONSTRAINT `$my_constraint` ON ()-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsReplace, oldSyntax = true))
  }

  test("CREATE OR REPLACE CONSTRAINT `$my_constraint` IF NOT EXISTS ON ()-[r:R]->() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsInvalidSyntax, oldSyntax = true))
  }

  test("CREATE CONSTRAINT `$my_constraint` IF NOT EXISTS ON ()<-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsDoNothing, oldSyntax = true))
  }

  test("CREATE CONSTRAINT `$my_constraint` ON ()-[r:R]-() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE CONSTRAINT my_constraint ON ()-[r:R]-() ASSERT (r.prop) IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("my_constraint"), IfExistsThrowError, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT `$my_constraint` ON ()-[r:R]-() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsReplace, oldSyntax = false))
  }

  test("CREATE OR REPLACE CONSTRAINT `$my_constraint` IF NOT EXISTS ON ()-[r:R]->() ASSERT (r.prop) IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsInvalidSyntax, oldSyntax = false))
  }

  test("CREATE CONSTRAINT `$my_constraint` IF NOT EXISTS ON ()<-[r:R]-() ASSERT r.prop IS NOT NULL") {
    yields(ast.CreateRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop"), Some("$my_constraint"), IfExistsDoNothing, oldSyntax = false))
  }

  test("CREATE CONSTRAINT my_constraint ON (node:Label) ASSERT node.prop IS NULL") {
    failsToParse
  }

  test("CREATE CONSTRAINT my_constraint ON ()-[r:R]-() ASSERT EXISTS (r.prop) IS NOT NULL") {
    failsToParse
  }

  test("CREATE CONSTRAINT $my_constraint ON ()-[r:R]-() ASSERT EXISTS (r.prop)") {
    failsToParse
  }

  // Drop constraint

  test("DROP CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NODE KEY") {
    yields(ast.DropNodeKeyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop"))))
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    yields(ast.DropNodeKeyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2"))))
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT node.prop IS UNIQUE") {
    yields(ast.DropUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop"))))
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT (node.prop) IS UNIQUE") {
    yields(ast.DropUniquePropertyConstraint(varFor("node"), labelName("Label"), Seq(prop("node", "prop"))))
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT (node.prop1,node.prop2) IS UNIQUE") {
    yields(ast.DropUniquePropertyConstraint(varFor("node"), labelName("Label"),
      Seq(prop("node", "prop1"), prop("node", "prop2"))))
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT EXISTS (node.prop)") {
    yields(ast.DropNodePropertyExistenceConstraint(varFor("node"), labelName("Label"), prop("node", "prop")))
  }

  test("DROP CONSTRAINT ON ()-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.DropRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop")))
  }

  test("DROP CONSTRAINT ON ()-[r:R]->() ASSERT EXISTS (r.prop)") {
    yields(ast.DropRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop")))
  }

  test("DROP CONSTRAINT ON ()<-[r:R]-() ASSERT EXISTS (r.prop)") {
    yields(ast.DropRelationshipPropertyExistenceConstraint(varFor("r"), relTypeName("R"), prop("r", "prop")))
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT node.prop IS NODE KEY") {
    failsToParse
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT EXISTS node.prop") {
    failsToParse
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT (node.prop) IS NOT NULL") {
    failsToParse
  }

  test("DROP CONSTRAINT ON (node:Label) ASSERT node.prop IS NOT NULL") {
    failsToParse
  }

  test("DROP CONSTRAINT ON ()-[r:R]-() ASSERT EXISTS r.prop") {
    failsToParse
  }

  test("DROP CONSTRAINT ON ()-[r:R]-() ASSERT (r.prop) IS NOT NULL") {
    failsToParse
  }

  test("DROP CONSTRAINT ON ()-[r:R]-() ASSERT r.prop IS NOT NULL") {
    failsToParse
  }

  test("DROP CONSTRAINT my_constraint") {
    yields(ast.DropConstraintOnName("my_constraint", ifExists = false))
  }

  test("DROP CONSTRAINT `$my_constraint`") {
    yields(ast.DropConstraintOnName("$my_constraint", ifExists = false))
  }

  test("DROP CONSTRAINT my_constraint IF EXISTS") {
    yields(ast.DropConstraintOnName("my_constraint", ifExists = true))
  }

  test("DROP CONSTRAINT $my_constraint") {
    failsToParse
  }

  test("DROP CONSTRAINT my_constraint ON (node:Label) ASSERT (node.prop1,node.prop2) IS NODE KEY") {
    failsToParse
  }

  // help methods

  private def propertyKeyName(name: String) = {
    expressions.PropertyKeyName(name)(pos)
  }
}
