/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource.embedded

/**
 * Had to add this class in order for org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryAutoConfiguration
 * to work since it has the conditional for the presence of the EmbeddedDatabaseType.class
 */
/**
 * A supported embedded database type.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
enum class EmbeddedDatabaseType {

    /** The [Hypersonic](http://hsqldb.org) Embedded Java SQL Database.  */
    HSQL,

    /** The [H2](https://h2database.com) Embedded Java SQL Database Engine.  */
    H2,

    /** The [Apache Derby](https://db.apache.org/derby) Embedded SQL Database.  */
    DERBY

}
