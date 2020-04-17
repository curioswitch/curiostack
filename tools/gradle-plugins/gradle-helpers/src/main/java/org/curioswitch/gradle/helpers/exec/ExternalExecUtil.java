/*
 * MIT License
 *
 * Copyright (c) 2020 Choko (choko@curioswitch.org)
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
package org.curioswitch.gradle.helpers.exec;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.process.ExecSpec;
import org.gradle.workers.WorkerExecutor;

/** Utility for invoking external programs. */
public final class ExternalExecUtil {

  /**
   * Executes an external program by customizing a {@link ExecSpec} with the supplied {@link
   * Action}. If the customized {@link ExecSpec} is serializable, the execution will happen in
   * parallel with others by running in {@link WorkerExecutor}, otherwise it will be invoked
   * serially.
   */
  public static void exec(
      Project project, WorkerExecutor workerExecutor, Action<? super ExecSpec> action) {
    var customizer = new ExecCustomizer(project);
    action.execute(customizer);
    if (customizer.isSerializable()) {
      project.getLogger().info("Executing command in worker executor.");
      workerExecutor
          .noIsolation()
          .submit(
              ExecWorkAction.class, parameters -> parameters.getExecCustomizer().set(customizer));
    } else {
      project.getLogger().info("Executing command serially.");
      project.exec(customizer::copyTo);
    }
  }

  private ExternalExecUtil() {}
}
