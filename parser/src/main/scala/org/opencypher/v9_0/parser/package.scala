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
package org.opencypher.v9_0

import org.opencypher.v9_0.parser.matchers.GlobbedIdentifierPartMatcher
import org.opencypher.v9_0.parser.matchers.GlobbedIdentifierStartMatcher
import org.opencypher.v9_0.parser.matchers.IdentifierPartMatcher
import org.opencypher.v9_0.parser.matchers.IdentifierStartMatcher
import org.opencypher.v9_0.parser.matchers.WhitespaceCharMatcher
import org.parboiled.scala.Rule0

package object parser {
  lazy val IdentifierStart: Rule0 = new IdentifierStartMatcher()
  lazy val GlobbedIdentifierStart: Rule0 = new GlobbedIdentifierStartMatcher()
  lazy val IdentifierPart: Rule0 = new IdentifierPartMatcher()
  lazy val GlobbedIdentifierPart: Rule0 = new GlobbedIdentifierPartMatcher()
  lazy val WSChar: Rule0 = new WhitespaceCharMatcher()
}
