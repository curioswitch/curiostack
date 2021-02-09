# @curiostack/nunjucks

An extended version of [mozilla/nunjucks](https://github.com/mozilla/nunjucks),
with some extra convenience methods added on.

## Usage

### Import

The extended `nunjucks` object can be imported in other `curiostack` `yarn` modules
using the package name `@curiostack/nunjucks`. It can work outside of curiostack as well,
but your project must use typescript and be configured to compile this module.

```typescript
import nunjucks from '@curiostack/nunjucks'
// no need for require('nunjucks')

const env = nunjucks.Environment()
// ...
```

### API

The underlying API of `nunjucks` is unchanged, so you can
[use the official docs](https://mozilla.github.io/nunjucks/getting-started.html) for reference.

## Environment

The [nunjucks Environment](https://mozilla.github.io/nunjucks/api.html#environment)
class still functions as before. It just has been extended with some extra functionality.

```typescript
import nunjucks from '@curiostack/nunjucks'

const env = nunjucks.Environment()
// ...

const result = env.render('template.njk')
// `result` is rendered with all of the base `nunjucks.Environment` logic
//   as well as the extended `curiostack` `nunjucks.Environment` functionality
```

> This means that you can NOT access the extended features, if you use the
> [simple nunjucks API](https://mozilla.github.io/nunjucks/api.html#simple-api),
> without initializing the extended `nunjucks.Environment`

### Filters

The `Environment` object is extended with some extra convenience filters.

#### allAfterLine(lineMatcher: string, [doInclusionCheck: boolean])

Scans the content of a multiline text and returns all the lines after a line
matching a `regex` expression.

Alternatively, if `doInclusionCheck` is `true`, the line is checked if it
includes `lineMatcher` within it, instead of doing a `regex` evaluation.

> The last line gets `\n` appended to it.
> If you wish, you can use `trim` to get rid of it.

##### input

```
{{ 'foo\na unique line\nbar\nbaz' | allAfterLine('.*unique.*') | trim }}
{{ 'foo\na unique line\nbar\nbaz' | allAfterLine('unique', true) | trim }}
```

##### output

```
bar\nbaz
bar\nbaz
```

#### allBetweenLines(lineMatcher: string, [doInclusionCheck: boolean])

Scans the content of a multiline text and returns all the lines between two lines
matching a `regex` expression.

Alternatively, if `doInclusionCheck` is `true`, the lines are checked if they
include `lineMatcher` within themselves, instead of doing a `regex` evaluation.

> The last line gets `\n` appended to it.
> If you wish, you can use `trim` to get rid of it.

##### input

```
{{ 'foo\nuniq\nbar\nbaz\nuniq\nfoz' | allBetweenLines('.*uniq.*') | trim }}
{{ 'foo\nuniq\nbar\nbaz\nuniq\nfoz' | allBetweenLines('uniq', true) | trim }}
```

##### output

```
bar\nbaz
bar\nbaz
```

## Development

All of the package code is under `common/web/nunjucks` and can be worked on there.

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
# automatically fix linting problems, and output problems which can't be auto fixed
```
