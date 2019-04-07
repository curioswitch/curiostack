/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.curioswitch.gradle.plugins.gcloud.keys.KmsKeyDecrypter

plugins {
    id("org.curioswitch.gradle-curio-database-plugin")
}

val keys: KmsKeyDecrypter by rootProject.extra
val devAdminPasswordEncrypted = "CiQAhAX+YPDiPB2yq0A5V5YZAKO0py1mbMW3Mun717Xs3CPJZMsSSQCggp6mP3bGNpHURfeMZDevsPK6DFz7gWvmb/0v/I7/mR1WF6zEIxTOCLldA9Ewii5WxadAk00CrjAF6JnW4SdYQWdqjmBNKcM="

database {
    dbName.set("cafemapdb")
    try {
        adminPassword.set(keys.decrypt(devAdminPasswordEncrypted))
    } catch (t: Throwable) {
        adminPassword.set("")
    }
}

flyway {
    url = "jdbc:mysql://google/cafemapdb?cloudSqlInstance=curioswitch-cluster:asia-northeast1:curioswitchdb-dev&socketFactory=com.google.cloud.sql.mysql.SocketFactory"
}
