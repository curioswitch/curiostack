/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
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
package org.curioswitch.gradle.documentation.template;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import groovy.lang.Closure;
import groovy.text.SimpleTemplateEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

public class TemplateProcessor {
  private final File templateFile;
  private final File templateFileDir;

  public TemplateProcessor(String templateFilePath) throws FileNotFoundException {
    this(new File(templateFilePath));
  }

  public TemplateProcessor(File templateFile) throws FileNotFoundException {
    if (!templateFile.isFile()) {
      throw new FileNotFoundException("Cannot find provided template file.");
    }

    this.templateFile = templateFile;
    this.templateFileDir = templateFile.getParentFile();
  }

  public String process() throws IOException, ClassNotFoundException {
    var binding = new HashMap<String, Object>();
    binding.put("include", taggedTextFileInclusion);
    var result = new StringWriter();
    new SimpleTemplateEngine().createTemplate(templateFile).make(binding).writeTo(result);

    return result.toString();
  }

  private final Closure<TaggableText> taggedTextFileInclusion = new Closure<>(this) {
    @Override
    public TaggableText call(Object path) {
      try {
        var file = new File(templateFileDir, path.toString());
        return file.isFile()
            ? new TaggableText(Files.asCharSource(file, Charsets.UTF_8).read())
            : null;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  };
}
