/*
 * Copyright © 2002-2020 Neo4j Sweden AB (http://neo4j.com)
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
package org.opencypher.v9_0.expressions.functions

import org.opencypher.v9_0.expressions.TypeSignature
import org.opencypher.v9_0.expressions.TypeSignatures
import org.opencypher.v9_0.util.symbols.CTInteger
import org.opencypher.v9_0.util.symbols.CTNumber
import org.opencypher.v9_0.util.symbols.CTString

case object ToInteger extends Function with TypeSignatures {
  override def name = "toInteger"

  override val signatures = Vector(
    TypeSignature(name, CTString, CTInteger, "Converts a string value to an integer value.", "Scalar"),
    TypeSignature(name, CTNumber, CTInteger, "Converts a floating point value to an integer value.", "Scalar")
  )
}
