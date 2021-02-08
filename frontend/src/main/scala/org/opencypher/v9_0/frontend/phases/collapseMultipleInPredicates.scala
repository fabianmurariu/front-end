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
package org.opencypher.v9_0.frontend.phases

import org.opencypher.v9_0.ast.semantics.SemanticFeature
import org.opencypher.v9_0.expressions
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.expressions.In
import org.opencypher.v9_0.expressions.ListLiteral
import org.opencypher.v9_0.expressions.Ors
import org.opencypher.v9_0.frontend.phases.factories.PlanPipelineTransformerFactory
import org.opencypher.v9_0.rewriting.conditions.SemanticInfoAvailable
import org.opencypher.v9_0.util.Rewriter
import org.opencypher.v9_0.util.StepSequencer
import org.opencypher.v9_0.util.bottomUp

import scala.collection.immutable.Iterable

case object EqualityRewrittenToIn extends StepSequencer.Condition
case object InPredicatesCollapsed extends StepSequencer.Condition

/*
This class merges multiple IN predicates into larger ones.
These can later be turned into index lookups or node-by-id ops
 */
case object collapseMultipleInPredicates extends StatementRewriter with StepSequencer.Step with PlanPipelineTransformerFactory {
  override def description: String = "merge multiple IN predicates into larger ones"

  case class InValue(lhs: Expression, expr: Expression)

  override def instance(ignored: BaseContext): Rewriter = bottomUp(Rewriter.lift {
    case predicate@Ors(exprs) =>
      // Find all the expressions we want to rewrite
      val (const: Seq[Expression], nonRewritable: Seq[Expression]) = exprs.toList.partition {
        case In(_, rhs: ListLiteral) => true
        case _ => false
      }

      // For each expression on the RHS of any IN, produce a InValue place holder
      val ins: Seq[InValue] = const.flatMap {
        case In(lhs, rhs: ListLiteral) =>
          rhs.expressions.map(expr => InValue(lhs, expr))
      }

      // Find all IN against the same predicate and rebuild the collection with all available values
      val groupedINPredicates = ins.groupBy(_.lhs)
      val flattenConst: Iterable[In] = groupedINPredicates.map {
        case (lhs, values) =>
          val pos = lhs.position
          expressions.In(lhs, ListLiteral(values.map(_.expr).toIndexedSeq)(pos))(pos)
      }

      // Return the original non-rewritten predicates with our new ones
      nonRewritable ++ flattenConst match {
        case head :: Nil => head
        case l => Ors(l)(predicate.position)
      }
  })

  override def preConditions: Set[StepSequencer.Condition] = Set(EqualityRewrittenToIn, BooleanPredicatesInCNF)

  override def postConditions: Set[StepSequencer.Condition] = Set(InPredicatesCollapsed)

  override def invalidatedConditions: Set[StepSequencer.Condition] = SemanticInfoAvailable // Introduces new AST nodes

  override def getTransformer(pushdownPropertyReads: Boolean,
                              semanticFeatures: Seq[SemanticFeature]): Transformer[BaseContext, BaseState, BaseState] = this
}
