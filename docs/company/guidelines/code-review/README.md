# Code Review Guidelines

Code quality is essential to a fast-moving organization - high quality code has less bugs, often 
runs faster, and can be changed more easily with respect to ever-changing product / industry 
requirements.  Code review is one of the fundamental processes of software engineering to support 
writing high quality code. This document lays out some common principles and guidelines so everyone 
can enjoy efficient and productive code reviews.

One thing to keep in mind - code review is only to provide constructive suggestions to achieve a 
shared goal of high code quality. It does not reflect personally on the author or reviewer - even 
the most experienced software engineers can sometimes make poor choices when writing code, and code 
review is a chance to get input from others from an outside view. Reviewers also may be missing 
context and may make incorrect statements. Both of these are normal and the content of a code review 
would never be used to evaluate an individual.

## General guidelines

### Required code review

* Any time making a code change, a PR with that change is opened and an approval from a code owner 
is required before merging it.

* The code owners can be checked [here](../../../../.github/CODEOWNERS), though if you're not sure 
you can leave off reviewers and they will be automatically assigned.

* Code owners are based on directory - for every directory an owner should approve the PR

    * GitHub will mark the PR as not-ready for merge until this happens in case you are confused 
    about the directory owners

    * For large PRs, especially refactoring ones, merging even when some directories haven't been 
    approved is fine if the changes are small

* PR authors that are themselves an owner of the affected code can consider that as approval from a 
code owner and merge after getting approval from a non-owner

    * GitHub does not recognize this - the PR will still be marked as not-ready for merge

    * It is still encouraged to get a review from an owner instead of non-owner when it wouldn't 
    significantly affect turnaround time

    * It is discouraged for an owner to merge without any review at all, but is allowed. This should 
    almost never be used for code change of several lines or more, and is generally used when just 
    updating a dependency version, etc. Use judgment, and when in doubt always get a review.

* If a significant code change was made, consider previous approvals to be invalidated - it is good 
to get another look, usually by replying with something like "Made ___ big change, PTAL"

    * PTAL is an acronym for Please Take Another Look

### Quick turnaround time

* Code cannot be merged until it has been reviewed - providing reasonably quick code reviews is 
important for unblocking other team members. A review may take many iterations, making this more 
important.

* People will have their personal fit for ensuring quick turnaround without too much disruption, but 
aim for responding to an assigned review within 2 hours.

    * If you need some more time to think about the change, for example because it's complex with 
    many affected parts, it's ok but aim to reply saying you will take some time to think about it 
    within the 2 hours.

    * Checking pending PRs when coming back from a coffee break usually results in a good turnaround 
    time

* The hard line for a code review iteration is 24 hours - it is strongly encouraged to respond 
within this time.

    * There are cases where workloads / outside factors make it difficult to respond to reviews on 
    time and this is perfectly fine. If you think you will not be able to make this time or do quick 
    iterations after that, reply to the PR saying you do not currently have time to do the review 
    and the author should find another reviewer.

    * After 24 hours, authors are free to, and should, reply to the PR with a ping to remind the 
    reviewer that they need to reply. If there is no response to the ping after a reasonable (use 
    judgment) amount of time, it is fine to message them for a direct reply, especially if other 
    work is blocked.

### Thoughtful responses

* Code review is to provide constructive feedback to improve the quality of the code. There is never 
any reason for a comment to be harsh or personal.

* It is good to provide any responses that come to mind - no comment is a bad one. Even if some 
misunderstanding means it's not correct, that will get clarified and no harm done. Though even 
better is these often lead to cleanups or extra code documentation to improve on the confusing 
nature of a piece of code.

* It is good to provide links to API documentation when suggesting the use of a library function.

### General points to look for when reviewing

* We prioritize readability over writability with code - code will generally be read far more often 
than written.

* Code should have good test coverage and follow idiomatic techniques to make the code easy to read 
and maintain.

* Technical debt is inevitable - if the author acknowledges a short-term good but long-term poor 
decision, it's fine and encourage them to leave a TODO and/or JIRA (whether JIRA is filed will 
depend on the size of the debt, use judgment).

* Language specific points will / are gathered in language-specific guidelines

## Tips for using GitHub pull requests for code review

### Enable email notifications

* Either enable Gmail desktop notifications or install an extension like [GitHub Notifier](https://chrome.google.com/webstore/detail/notifier-for-github/lmjdlojahmbbcodnpecnjnmlddbkjhnn?hl=en)	

* If using Gmail, having a filter for relevant notifications can help - Matches: from:(github.com) 
to:(author@noreply.github.com OR review_requested@noreply.github.com OR assigned@noreply.github.com 
OR mention@noreply.github.com)

### Avoid unnecessary email notifications. 

* Use the review feature to leave multiple comments at the same time before submitting all at once.

* Comments with no content, such as "Done" are not needed. If leaving them, make sure they are part 
of a review so it's not sent individually.

* It is fine to just leave a single PR-wide comment "Updated" without responding to any comments if 
the responses would be trivial.

* If you don't respond that you have addressed comments, you may not get another iteration. Be clear 
by replying "Updated" at least.

### Ensure reviewers have appropriate context

* Add a JIRA issue key in the title of the PR, e.g., CURIO-100 Add codelabs. This will automatically 
link the PR to the JIRA issue.

* For medium or long length PRs, add a detailed description of what the code does and is intended to 
solve. This can be a good opportunity for the author to reflect on whether the code ended up as they 
intended and gives reviewers good background when reviewing. A good 
[example](https://github.com/line/armeria/pull/1148) can be PRs in the armeria repository. For 
smaller PRs, it's ok to leave out a description under the assumption that it is fairly obvious - 
worst case a reviewer will ask about confusing points. 

* In the description, make sure to list out any followup PRs you have planned or limitations you 
already have considered so reviewers know not to focus on them.

### Use WIP PRs for early feedback

* It is fine to get early feedback on code before it is ready for full code review / merging.

* Add something like [WIP] to the PR title to make clear it is not ready for merging.

* Avoid using [WIP] when you don't want early feedback - if you just want to see a diff in GitHub, 
you can use "create PR" without actually sending it out. If you want the diff to persist for some 
time, it is also ok to finish creating the PR but immediately close it so reviewers know now to look 
at it - the diff will still stay.

* Never merge a PR with [WIP] in the title.

### Merge when ready

* We expect the PR author, not reviewer, to merge PRs. This is to give time to authors to resolve 
any last-minute comments or solve merge conflicts.

    * These final changes will generally not be reviewed. Be careful and if you think you made 
    significant changes, ask for another review.

* In general, a PR is ready to merge after GitHub marks it as green - this takes into account 
approvals and continuous build.

* Make a quick check to make sure there are no outstanding comments. People may add minor 
suggestions along with their approval.

* If a significant code change was made after approval, just being green may not be enough - if you 
think reviewers should take another look even though it's approved, ask for it.

* Check the PR title is still correct - many times changes happen during code review and the title 
doesn't reflect the final state. Update the title before merging so the git history displays the 
correct information.

## Working in Open Source

At CurioSwitch, we are developing everything in the open, to ensure all best practices can be shared
with developers. For other organizations, though, it is common to have private and public
repositories. These guidelines can help managing some of the pain when context-switching between
private and public.

* Check the name of the repository when opening a pull request. Github's email notifications have 
the repository name as the beginning of the subject.

    * [yourorg/yourrepo] is our primary private repository and it is rare to work in any other of 
    our private repositories. If you open a pull request for a different repository, assume first 
    that it is public.

* Avoid mentioning confidential topics on public pull requests. This is usually company names or 
specific long-term development plans

    * It is often reasonable to replace such words with general terms, e.g., "the partner" instead 
    of "Partner Name"

    * If a topic does need discussion, start a separate email thread for it on eng-all@. It is 
    highly unlikely that such a longer discussion needs to happen within Github since it rarely 
    affects a single line of code

* Prioritize clear, detailed documentation in open source projects

    * Documentation is all code + non-code text - README, docs, and even code comments.

    * Assume the reader is not familiar with what they are reading. Developer platforms succeed when 
    the documentation teaches the developer something new.

    * Err on the side of overdocumenting vs underdocumenting for open source projects.

    * Try to make sure grammar / spelling are correct. With internal code, we may ignore very minor 
    mistakes, especially in code comments, but for open source pull requests, point out every single 
    tiny mistake.

