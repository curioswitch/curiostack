# @curiostack/nunjucks

An extended version of [mozilla/nunjucks](https://github.com/mozilla/nunjucks),
with some extra convenience methods added on.

## Usage

### Import

The default option for importing is, the extended version of `nunjucks` `Environment`.
Also `Template`, `Loader`, `FileSystemLoader`, `WebLoader` are available as non-default
imports.

The import can be done in other `curiostack` `yarn` modules  using the package name
`@curiostack/nunjucks-extended`. It can work outside of curiostack as well, but your project
must use typescript and be configured to compile this module.

```typescript
import Environment, { Template, Loader, FileSystemLoader, WebLoader } from '@curiostack/nunjucks-extended';
// no need for require('nunjucks')

const env = new Environment();
// alternatives:
// const env = new Environment(new FileSystemLoader('/some/path'));
// const template = new Template('Hello {{ username }}', env);
// etc ...
```

### API

The underlying API of each available `nunjucks` class is unchanged, so you can
[use the official docs](https://mozilla.github.io/nunjucks/getting-started.html) for reference.

## Environment

The [nunjucks Environment](https://mozilla.github.io/nunjucks/api.html#environment)
class still functions as before. It just has been extended with some extra functionality.

```typescript
import Environment from '@curiostack/nunjucks-extended';

const env = new Environment();
// ...

const result = env.render('template.njk');
// `result` is rendered with all of the base `nunjucks.Environment` logic
//   as well as the extended `curiostack` `Environment` functionality
```

### Filters

The `Environment` object is extended with some extra convenience filters.

#### allAfterLine(lineMatcher: string, [exactMatch: boolean])

Scans the content of a multiline text and returns all the text after a line
matching a `regex` expression.

If `exactMatch` is `true`, a line will be matched only when all of it matches
the `regex` expression exactly.

##### input

```
{{ 'foo\n_@@_\nbar\nbaz' | allAfterLine('@') }}
{{ 'foo\n_@@_\nbar\nbaz' | allAfterLine('^_@@_$', true) }}
```

##### output

```
bar\nbaz
bar\nbaz
```

#### allBetweenLines(lineMatcher: string, [exactMatch: boolean])

Scans the content of a multiline text and returns all the text between two lines
matching a `regex` expression.

If `exactMatch` is `true`, a line will be matched only when all of it matches
the `regex` expression exactly.

> If multiple blocks are found, all of them are joined together with a newline (`\n`)
> and returned as one block.

##### input

```
{{ 'foo\n_@@_\nbar\nbaz\n_@@_\nfoz' | allBetweenLines('@') }}
{{ 'foo\n_@@_\nbar\nbaz\n_@@_\nfoz' | allBetweenLines('^_@@_$', true) }}
{{ 'foo\n_@@_\nbar\nbaz\n_@@_\nfoz\n_@@_\nbop\n_@@_\nbap' | allBetweenLines('@') }}
```

##### output

```
bar\nbaz
bar\nbaz
bar\nbaz\nbop
```

### Testing

`jest` is used for tests, and can be run with the following command.

```shell
./gradlew :common:web:nunjucks:yarn_test
```

### Style

`eslint` with `curiostack` rules is used for controlling code style, and can be run
with the following command.

```shell
./gradlew :common:web:nunjucks:yarn_lint
# output linting problems

./gradlew :common:web:nunjucks:yarn_lint_--fix
# automatically fix linting problems, and output problems which can't be auto-fixed
```
