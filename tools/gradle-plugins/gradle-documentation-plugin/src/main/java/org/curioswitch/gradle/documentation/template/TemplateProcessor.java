package org.curioswitch.gradle.documentation.template;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import groovy.lang.Closure;
import groovy.text.SimpleTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

public class TemplateProcessor {
  private final File templateFile;
  private final File templateFileDir;

  public TemplateProcessor(String templateFilePath) {
    this(new File(templateFilePath));
  }

  public TemplateProcessor(File templateFile) {
    if (!templateFile.isFile()) {
      throw new IllegalArgumentException("Provided template file does not exist.");
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
