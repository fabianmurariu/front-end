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
package org.opencypher.v9_0.ast.factory.neo4j

import org.opencypher.v9_0.util.test_helpers.TestName
import org.scalatest.FunSuiteLike

class RoleAdministrationCommandJavaCcParserTest extends ParserComparisonTestBase with FunSuiteLike with TestName {

  test("USE GRAPH SYSTEM SHOW ROLES") {
    assertSameAST(testName)
  }

  test("SHOW ROLES") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW ALL ROLES") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW POPULATED ROLES") {
    assertSameAST(testName)
  }

  test("SHOW ROLES WITH USERS") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW ALL ROLES WITH USERS") {
    assertSameAST(testName)
  }

  test("SHOW POPULATED ROLES WITH USERS") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW ALL ROLES YIELD role") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW ALL ROLES WHERE role='PUBLIC'") {
    assertSameAST(testName)
  }

  test("SHOW ALL ROLES YIELD role RETURN role") {
    assertSameAST(testName)
  }

  test("SHOW ALL ROLES YIELD role RETURN") {
    assertJavaCCException(testName, "Invalid input '': expected \"DISTINCT\", \"*\" or an expression (line 1, column 33 (offset: 32))")
  }

  test("SHOW POPULATED ROLES YIELD role WHERE role='PUBLIC' RETURN role") {
    assertSameAST(testName)
  }

  test("SHOW POPULATED ROLES YIELD * RETURN *") {
    assertSameAST(testName)
  }

  test("SHOW ROLES WITH USERS YIELD * LIMIT 10 WHERE foo='bar' RETURN some,columns LIMIT 10") {
    assertSameAST(testName)
  }

  test("SHOW ALL ROLES YIELD return, return RETURN return") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW ROLE") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ALL\", \"POPULATED\" or \"ROLES\" (line 1, column 14 (offset: 13))")
  }

  test("SHOW ALL ROLE") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ROLES\" (line 1, column 10 (offset: 9))")
  }

  test("SHOW POPULATED ROLE") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ROLES\" (line 1, column 16 (offset: 15))")
  }

  test("SHOW ROLE role") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ALL\", \"POPULATED\" or \"ROLES\" (line 1, column 6 (offset: 5))")
  }

  test("SHOW ROLE WITH USERS") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ALL\", \"POPULATED\" or \"ROLES\" (line 1, column 6 (offset: 5))")
  }

  test("CATALOG SHOW ROLES WITH USER") {
    assertJavaCCException(testName, "Invalid input 'USER': expected \"USERS\" (line 1, column 25 (offset: 24))")
  }

  test("SHOW ROLE WITH USER") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ALL\", \"POPULATED\" or \"ROLES\" (line 1, column 6 (offset: 5))")
  }

  test("SHOW ALL ROLE WITH USERS") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ROLES\" (line 1, column 10 (offset: 9))")
  }

  test("SHOW ALL ROLES WITH USER") {
    assertJavaCCException(testName, "Invalid input 'USER': expected \"USERS\" (line 1, column 21 (offset: 20))")
  }

  test("SHOW ALL ROLE WITH USER") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ROLES\" (line 1, column 10 (offset: 9))")
  }

  test("YIELD a, b, c WHERE a = b") {
    assertSameAST(testName)
  }

  test("SHOW POPULATED ROLES YIELD role ORDER BY role SKIP -1") {
    assertSameAST(testName)
  }

  test("SHOW POPULATED ROLES YIELD role ORDER BY role SKIP -1*4 + 2") {
    assertSameAST(testName)
  }

  test("SHOW POPULATED ROLES YIELD role ORDER BY role LIMIT -1") {
    assertSameAST(testName)
  }

  test("CATALOG SHOW POPULATED ROLE WITH USERS") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ROLES\" (line 1, column 24 (offset: 23))")
  }

  test("CATALOG SHOW POPULATED ROLES WITH USER") {
    assertJavaCCException(testName, "Invalid input 'USER': expected \"USERS\" (line 1, column 35 (offset: 34))")
  }

  test("CATALOG SHOW POPULATED ROLE WITH USER") {
    assertJavaCCException(testName, "Invalid input 'ROLE': expected \"ROLES\" (line 1, column 24 (offset: 23))")
  }

  test("CATALOG SHOW ROLES WITH USER user") {
    assertJavaCCException(testName, "Invalid input 'USER': expected \"USERS\" (line 1, column 25 (offset: 24))")
  }

  test("SHOW POPULATED ROLES YIELD *,blah RETURN role") {
    val newline = System.getProperty("line.separator")
    val exceptionMessage =
      s"""Invalid input ',': expected $newline  <EOF>$newline  "RETURN"$newline  "WHERE"$newline  "ORDER"$newline  "SKIP"$newline  "LIMIT" (line 1, column 29 (offset: 28))"""
    assertJavaCCException(testName, exceptionMessage)
  }

  //  Creating roles

  test("CREATE ROLE foo") {
    assertSameAST(testName)
  }

  test("CREATE ROLE $foo") {
    assertSameAST(testName)
  }

  test("CATALOG CREATE ROLE `fo!$o`") {
    assertSameAST(testName)
  }

  test("CREATE ROLE ``") {
    assertSameAST(testName)
  }

  test("CREATE ROLE foo AS COPY OF bar") {
    assertSameAST(testName)
  }

  test("CREATE ROLE foo AS COPY OF $bar") {
    assertSameAST(testName)
  }

  test("CREATE ROLE foo AS COPY OF ``") {
    assertSameAST(testName)
  }

  test("CREATE ROLE `` AS COPY OF bar") {
    assertSameAST(testName)
  }

  test("CREATE ROLE foo IF NOT EXISTS") {
    assertSameAST(testName)
  }

  test("CREATE ROLE foo IF NOT EXISTS AS COPY OF bar") {
    assertSameAST(testName)
  }

  test("CREATE OR REPLACE ROLE foo") {
    assertSameAST(testName)
  }

  test("CREATE OR REPLACE ROLE foo AS COPY OF bar") {
    assertSameAST(testName)
  }

  test("CREATE OR REPLACE ROLE foo IF NOT EXISTS") {
    assertSameAST(testName)
  }

  test("CREATE OR REPLACE ROLE foo IF NOT EXISTS AS COPY OF bar") {
    assertSameAST(testName)
  }

  test("CATALOG CREATE ROLE \"foo\"") {
    assertSameAST(testName)
  }

  test("CREATE ROLE f%o") {
    assertSameAST(testName)
  }

  test("CREATE ROLE  IF NOT EXISTS") {
    assertSameAST(testName)
  }

  test("CREATE ROLE foo IF EXISTS") {
    assertSameAST(testName)
  }

  test("CREATE OR REPLACE ROLE") {
    assertJavaCCException(testName, "Invalid input '': expected a parameter or an identifier (line 1, column 23 (offset: 22))")
  }

  test("CREATE ROLE foo AS COPY OF") {
    assertJavaCCException(testName, "Invalid input '': expected a parameter or an identifier (line 1, column 27 (offset: 26))")
  }

  test("CREATE ROLE foo IF NOT EXISTS AS COPY OF") {
    assertJavaCCException(testName, "Invalid input '': expected a parameter or an identifier (line 1, column 41 (offset: 40))")
  }

  test("CREATE OR REPLACE ROLE foo AS COPY OF") {
    assertJavaCCException(testName, "Invalid input '': expected a parameter or an identifier (line 1, column 38 (offset: 37))")
  }

  test("CREATE ROLE foo UNION CREATE ROLE foo2") {
    assertJavaCCException(testName, "Invalid input 'UNION': expected <EOF>, \"AS\" or \"IF\" (line 1, column 17 (offset: 16))")
  }

  //  Dropping role

  test("DROP ROLE foo") {
    assertSameAST(testName)
  }

  test("DROP ROLE $foo") {
    assertSameAST(testName)
  }

  test("DROP ROLE ``") {
    assertSameAST(testName)
  }

  test("DROP ROLE foo IF EXISTS") {
    assertSameAST(testName)
  }

  test("DROP ROLE `` IF EXISTS") {
    assertSameAST(testName)
  }

  test("DROP ROLE ") {
    assertJavaCCException(testName, "Invalid input '': expected a parameter or an identifier (line 1, column 10 (offset: 9))")
  }

  test("DROP ROLE  IF EXISTS") {
    assertSameAST(testName)
  }

  test("DROP ROLE foo IF NOT EXISTS") {
    assertSameAST(testName)
  }

  //  Granting and Revoking role(s)

  Seq("ROLE", "ROLES").foreach {
    roleKeyword =>

      Seq(
        ("GRANT", "TO"),
        ("REVOKE", "FROM")
      ).foreach {
        case (verb: String, preposition: String) =>

          test(s"$verb $roleKeyword foo $preposition abc") {
            assertSameAST(testName)
          }

          test(s"CATALOG $verb $roleKeyword foo $preposition abc") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword " +
            s"catalog, show, populated, roles, role, users, replace, grant, revoke, if, copy, of, to " +
            s"$preposition abc") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword foo $preposition abc, def") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword foo,bla,roo $preposition bar, baz,abc,  def") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword `fo:o` $preposition bar") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword foo $preposition `b:ar`") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword `$$f00`,bar $preposition abc,`$$a&c`") {
            assertSameAST(testName)
          }

          // Should fail to parse if not following the pattern $command $roleKeyword role(s) $preposition user(s)

          test(s"$verb $roleKeyword") {
            assertJavaCCExceptionStart(testName, "Invalid input '': expected a parameter or an identifier")
          }

          test(s"$verb $roleKeyword foo") {
            assertJavaCCExceptionStart(testName, s"""Invalid input '': expected "$preposition" or ","""")
          }

          test(s"$verb $roleKeyword foo $preposition") {
            assertJavaCCExceptionStart(testName, "Invalid input '': expected a parameter or an identifier")
          }

          test(s"$verb $roleKeyword $preposition abc") {
            assertSameAST(testName)
          }

          // Should fail to parse when invalid user or role name

          test(s"$verb $roleKeyword fo:o $preposition bar") {
            assertSameAST(testName)
          }

          test(s"$verb $roleKeyword foo $preposition b:ar") {
            assertSameAST(testName)
          }
      }

      // Should fail to parse when mixing TO and FROM

      test(s"GRANT $roleKeyword foo FROM abc") {
        assertSameAST(testName)
      }

      test(s"REVOKE $roleKeyword foo TO abc") {
        assertSameAST(testName)
      }

      // ROLES TO USER only have GRANT and REVOKE and not DENY

      test(s"DENY $roleKeyword foo TO abc") {
        // temporary error message until remaining administration commmands are ported
        assertJavaCCExceptionStart(testName, "Invalid input 'DENY'")
      }
  }

  test("GRANT ROLE $a TO $x") {
    assertSameAST(testName)
  }

  test("REVOKE ROLE $a FROM $x") {
    assertSameAST(testName)
  }

  test("GRANT ROLES a, $b, $c TO $x, y, z") {
    assertSameAST(testName)
  }

  test("REVOKE ROLES a, $b, $c FROM $x, y, z") {
    assertSameAST(testName)
  }
}
