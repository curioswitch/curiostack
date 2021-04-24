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
import inquirer, { DistinctQuestion } from 'inquirer';

import {
  componentExists,
  convertTypeArg,
  renderTemplate,
  Type,
} from '../utils';

// eslint-disable-next-line
const packageJson = require('../../../../package.json');

const readFile = promisify(fs.readFile);

let argName: string | undefined;

program
  .version(packageJson.version)
  .arguments('[name]')
  .action((name: string) => {
    argName = name;
  })
  .option(
    '-t, --type <type>',
    'type of the component (normal|pure)',
    convertTypeArg,
  )
  .option('-h, --headers', 'whether to generate headers')
  .option('-a, --actions', 'whether to generate actions / reducer')
  .option('-s, --saga', 'whether to generate sagas')
  .option('-m, --messages', 'whether to generate i18n messages')
  .parse(process.argv);

async function run() {
  const actionsTemplate = await readFile(require.resolve('./actions.ts.hbs'));
  const classTemplate = await readFile(require.resolve('./class.tsx.hbs'));
  const loaderTemplate = await readFile(require.resolve('./loader.ts.hbs'));
  const messagesTemplate = await readFile(require.resolve('./messages.ts.hbs'));
  const reducerTemplate = await readFile(require.resolve('./reducer.ts.hbs'));
  const sagaTemplate = await readFile(require.resolve('./saga.ts.hbs'));
  const selectorsTemplate = await readFile(
    require.resolve('./selectors.ts.hbs'),
  );

  const questions: DistinctQuestion[] = [];
  let type: Type | undefined = program.type;
  if (!type) {
    questions.push({
      type: 'list',
      name: 'type',
      message: 'Select the type of the component',
      default: 'React.PureComponent',
      choices: ['React.PureComponent', 'React.Component'],
    });
  }
  let name = argName;
  if (!name) {
    questions.push({
      name: 'name',
      message: 'What should it be called?',
      validate: (value: any) => {
        if (/.+/.test(value)) {
          return componentExists(value)
            ? 'A component or container with this name already exists'
            : true;
        }
        return 'The name is required';
      },
    });
  }
  let wantHeaders = program.headers;
  if (wantHeaders === undefined) {
    questions.push({
      type: 'confirm',
      name: 'wantHeaders',
      default: true,
      message: 'Do you want headers (i.e., react-helmet)?',
    });
  }
  let wantActionsAndReducer = program.actions;
  if (wantActionsAndReducer === undefined) {
    questions.push({
      type: 'confirm',
      name: 'wantActionsAndReducer',
      default: true,
      message:
        'Do you want an actions/constants/selectors/reducer tuple for this container?',
    });
  }
  let wantSaga = program.saga;
  if (wantSaga === undefined) {
    questions.push({
      type: 'confirm',
      name: 'wantSaga',
      default: true,
      message: 'Do you want sagas for asynchronous flows? (e.g. fetching data)',
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
    if (answers.wantHeaders) {
      wantHeaders = answers.wantHeaders;
    }
    if (answers.wantActionsAndReducer) {
      wantActionsAndReducer = answers.wantActionsAndReducer;
    }
    if (answers.wantSaga) {
      wantSaga = answers.wantSaga;
    }
    if (answers.wantMessages !== undefined) {
      wantMessages = answers.wantMessages;
    }
  }
  const context = {
    name,
    type,
    wantHeaders,
    wantActionsAndReducer,
    wantSaga,
    wantMessages,
  };
  const componentDir = path.resolve(process.cwd(), `src/containers/${name}`);
  await mkdirs(path.resolve(componentDir, 'tests'));
  await renderTemplate(
    classTemplate,
    path.resolve(componentDir, 'index.tsx'),
    context,
  );
  await renderTemplate(
    loaderTemplate,
    path.resolve(componentDir, 'loader.ts'),
    context,
  );
  if (wantMessages) {
    await renderTemplate(
      messagesTemplate,
      path.resolve(componentDir, 'messages.ts'),
      context,
    );
  }
  if (wantActionsAndReducer) {
    await renderTemplate(
      actionsTemplate,
      path.resolve(componentDir, 'actions.ts'),
      context,
    );

    await renderTemplate(
      selectorsTemplate,
      path.resolve(componentDir, 'selectors.ts'),
      context,
    );

    await renderTemplate(
      reducerTemplate,
      path.resolve(componentDir, 'reducer.ts'),
      context,
    );
  }
  if (wantSaga) {
    await renderTemplate(
      sagaTemplate,
      path.resolve(componentDir, 'saga.ts'),
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
