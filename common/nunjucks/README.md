# @curiostack/nunjucks

An extended version of [mozilla/nunjucks](https://github.com/mozilla/nunjucks),
with some extra convenience methods added on.

## Usage

### Import

The extended `nunjucks` object can be imported in other `curiostack` `yarn` modules
using the package name `@curiostack/nunjucks`.

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

#### allAfterTaggedLine(tag: string, [useRegex: boolean])

Scans the content of a multiline text and returns all the lines after a tagged line.

A tagged line is a line which contains the `tag` text within it.

Alternatively, if `useRegex` is `true`, the tagged line is a line
which matches the `tag` as a `regex` expression exactly.
A partial match will not count.

> The last line gets `\n` appended to it.
> If you wish, you can use `trim` to get rid of it.

##### input

```
{{ 'foo\na unique line\nbar\nbaz' | allAfterTaggedLine('unique') | trim }}
{{ 'foo\na unique line\nbar\nbaz' | allAfterTaggedLine('.*unique.*', true) | trim }}
```

##### output

```
bar\nbaz
bar\nbaz
```

#### allBetweenTaggedLines(startTag: string, endTag: string, [useRegex: boolean])

Scans the content of a multiline text and returns all the lines between two tagged lines.

A tagged line is a line which contains the `startTag|endTag` text within it.

Alternatively, if `useRegex` is `true`, the tagged line is a line
which matches the `startTag|endTag` as a `regex` expression exactly.
A partial match will not count.

> The last line gets `\n` appended to it.
> If you wish, you can use `trim` to get rid of it.

##### input

```
{{ 'foo\nuniq1\nbar\nbaz\nuniq2\nfoz' | allBetweenTaggedLines('q1', 'q2') | trim }}
{{ 'foo\nuniq1\nbar\nbaz\nuniq2\nfoz' | allBetweenTaggedLines('.*q1.*', '.*q2.*', true) | trim }}
```

##### output

```
bar\nbaz
bar\nbaz
```

## Development

All of the package code is under `common/nunjucks` and can be worked on there.

### Testing

`jest` is used for tests and can be run with the following command.

```shell
./gradlew :common:nunjucks:yarn_test
```
