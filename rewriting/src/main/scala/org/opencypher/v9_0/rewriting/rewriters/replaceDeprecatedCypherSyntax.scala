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
package org.opencypher.v9_0.rewriting.rewriters

import org.opencypher.v9_0.rewriting.Deprecations
import org.opencypher.v9_0.util.Rewriter
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.StepSequencer.Condition
import org.opencypher.v9_0.util.StepSequencer.Step
import org.opencypher.v9_0.util.bottomUp

case object DeprecatedSyntaxReplaced extends Condition

case class replaceDeprecatedCypherSyntax(deprecations: Deprecations) extends Rewriter {
  override def apply(that: AnyRef): AnyRef = instance(that)
  val instance: Rewriter = bottomUp(Rewriter.lift(deprecations.find.andThen(d => d.generateReplacement())))
}

object replaceDeprecatedCypherSyntax extends Step {
  override def preConditions: Set[StepSequencer.Condition] = Set.empty

  override def postConditions: Set[StepSequencer.Condition] = Set(DeprecatedSyntaxReplaced)

  override def invalidatedConditions: Set[StepSequencer.Condition] = Set.empty
}