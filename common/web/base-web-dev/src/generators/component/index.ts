#!/usr/bin/env node
/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import fs from 'fs';
import { mkdirs } from 'fs-extra';
import path from 'path';
import { promisify } from 'util';

import program from 'commander';
import inquirer, { Question } from 'inquirer';

import { componentExists, renderTemplate } from '../utils';

import packageJson from '../../../package.json';

const readFile = promisify(fs.readFile);

let argName: string | undefined;

function convertType(type: 'normal' | 'stateless' | 'pure'): string {
  switch (type) {
    case 'normal':
      return 'React.Component';
    case 'stateless':
      return 'Stateless Function';
    case 'pure':
      return 'React.PureComponent';
    default:
      throw new Error('Unknown type: ' + type);
  }
}

program
  .version(packageJson.version)
  .arguments('[name]')
  .action((name: string) => (argName = name))
  .option(
    '-t, --type <type>',
    'type of the component (normal|stateless|pure)',
    convertType,
  )
  .option('-m, --messages', 'whether to generate i18n messages')
  .parse(process.argv);

async function run() {
  const classTemplate = await readFile(require.resolve('./class.tsx.hbs'));
  const messagesTemplate = await readFile(require.resolve('./messages.ts.hbs'));
  const statelessTemplate = await readFile(
    require.resolve('./stateless.tsx.hbs'),
  );
  const storiesTemplate = await readFile(require.resolve('./stories.tsx.hbs'));
  const testTemplate = await readFile(require.resolve('./test.tsx.hbs'));

  const questions: Question[] = [];
  let type = program.type;
  if (!type) {
    questions.push({
      type: 'list',
      name: 'type',
      message: 'Select the type of the component',
      default: 'React.PureComponent',
      choices: ['Stateless Function', 'React.PureComponent', 'React.Component'],
    });
  }
  let name = argName;
  if (!name) {
    questions.push({
      name: 'name',
      message: 'What should it be called?',
      validate: (value) => {
        if (/.+/.test(value)) {
          return componentExists(value)
            ? 'A component or container with this name already exists'
            : true;
        }
        return 'The name is required';
      },
    });
  }
  let wantMessages = program.messages;
  if (wantMessages === undefined) {
    questions.push({
      type: 'confirm',
      name: 'wantMessages',
      default: true,
      message:
        'Do you want i18n messages (i.e., will this component use text)?',
    });
  }

  if (questions.length > 0) {
    const answers = await inquirer.prompt(questions);
    if (answers.type) {
      type = answers.type;
    }
    if (answers.name) {
      name = answers.name;
    }
    if (answers.wantMessages !== undefined) {
      wantMessages = answers.wantMessages;
    }
  }

  const context = {
    name,
    type,
    wantMessages,
  };
  const componentDir = path.resolve(process.cwd(), `src/components/${name}`);
  await mkdirs(path.resolve(componentDir, 'stories'));
  await mkdirs(path.resolve(componentDir, 'tests'));
  if (type === 'Stateless Function') {
    await renderTemplate(
      statelessTemplate,
      path.resolve(componentDir, 'index.tsx'),
      context,
    );
  } else {
    await renderTemplate(
      classTemplate,
      path.resolve(componentDir, 'index.tsx'),
      context,
    );
  }
  await renderTemplate(
    testTemplate,
    path.resolve(componentDir, 'tests/index.test.tsx'),
    context,
  );
  await renderTemplate(
    storiesTemplate,
    path.resolve(componentDir, 'stories/index.tsx'),
    context,
  );
  if (wantMessages) {
    await renderTemplate(
      messagesTemplate,
      path.resolve(componentDir, 'messages.ts'),
      context,
    );
  }
}

run()
  .then(() => {
    process.exit(0);
  })
  .catch((err) => {
    console.log('Error running script.', err);
    process.exit(1);
  });
