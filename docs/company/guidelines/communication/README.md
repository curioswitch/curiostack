# Communication Guidelines

This document outlines core guidelines for communication at CurioSwitch, including information 
sharing and discussion. Many tools exist to support communication, and these guidelines try to 
present best practices to achieve efficient communication to maximize productivity and general 
happiness.

## Motivation

Effective communication is essential for creating a productive work environment that can quickly 
deliver high quality products to consumers. There are some guiding principles that we consider 
important to product development teams.

* Recorded. As much as possible, communication should be recorded in some form (meeting notes, or 
having a written discussion in the first place) so that the important decisions and action items do 
not get lost over time.

* Transparent. There is very little communication that needs to be kept a secret from other 
employees of the company (hiring and investment talks are the two prime, and likely the only, 
counterexamples). By giving wide access to communication records to employees, people can have a 
better understanding of the current state of topics, and can see the result of decision making and 
other discussions.

* Targeted. On the flip side of transparency, it is also important that proactive communication 
happens only to members who are related. If all information in the company is channeled to everyone 
in an unstructured way, it is difficult for receivers to pick out truly relevant information. 
Achieving both transparency and targeting at the same time tends to be the largest challenge of 
communication.

* Non-interruptive. Very few communication requires an extremely quick response. We want to allow 
receivers of information to process at their own pace rather than pushing for synchronous 
conversation for tasks that can be done asynchronously and out of order.

The intent of these principles is to encourage an atmosphere where it is very easy to share 
information, without causing information overload. Members can find information when they need it, 
while focusing on relevant topics or just go back to work otherwise. They should have control over 
their communication, not be controlled by it, and we encourage using tools to help achieve this.

## Tool usage

* File a JIRA ticket for any topic that has a well defined start and finish. High-level discussion 
should generally be done on the JIRA ticket itself.

* For any other topic, use Hangouts Chat or in-person talking when you expect a reply within one 
minute.

* For any other case, use email. In particular, for announcements, use email.

## General

These are general guidelines that can apply to communication in a variety of tools. They intend to 
minimize confusion that can happen when communication is not consistent.

* Write numeric dates in order of Year, Month, Day. For the date September, 4, 2018, the following 
are all fine, we are mostly concerned with having consistent order. When in doubt, the first format 
listed is usually most convenient since it is sortable in alphabetical order.

    * 2018/09/04

    * 2018/9/4

    * 2018-9-04

## Email

Email allows for asynchronous communication and fine-grained targeting. Using email for general 
information sharing is highly encouraged to allow consumers of the information to do so at their 
best timing.

### Creating mailing lists

We use mailing lists heavily to 1) allow targeting multiple people, usually a team, at the same time 
and 2) to leave a record of all communication. The guidelines for maintaining mailing lists are

* Each team has a mailing list including all its members, e.g., eng-all@, sales-all@, support-all@.

* If a team uses temps, contractors, or vendors (TVC), they should maintain lists to be able to do 
sensitive communication that should only be seen by Full-Time Employees (FTEs), e.g. eng-fte@ and 
eng-tvc@. Teams that don't have TVCs can just use the single -all@ list. 

    * When maintaining three lists, -fte@ and -tvc@ should be the only members of the -all@ team 
    mailing list, and individual membership can be managed in the respective groups.

* When creating larger groups (e.g., organizational ones), avoid adding members directly to groups 
but compose them of multiple mailing lists. There should be very few cases of members being part of 
a mailing list, except for their team mailing list.

* Each team should have a minutes mailing list for sharing team meeting notes, e.g. eng-minutes@, 
sales-minutes@, support-minutes@.

    * The minutes mailing list must have meeting-minutes@ as a member. This ensures all notes sent 
    to the team's minutes list also gets shared to the office-wide one (it is not necessary to 
    address both lists when sending minutes as it will automatically be shared)

    * The minutes mailing list must also include the team mailing list as a member to ensure team 
    members always see the minutes. 

    * All meetings must have notes, including a clear agenda, and all notes must be shared to the 
    minutes mailing list. This ensures transparency into discussions and decision-making.

        * The only exception is highly sensitive content like hiring or investment discussions.

### Discussion on email

Discussion on emails are meant to be targeted at relevant members while also being open to related 
members.

* Most discussions have clear owners (team lead, specific member working on a task, etc) and all 
such owners should be in the *to:* addresses of the email.

* All discussion emails should also be sent to relevant mailing lists to make sure they are 
collected for future reference and searchability. It is common to have multiple mailing lists for 
cross-team discussions. All such mailing lists should be added as *cc:* addresses

    * It is extremely rare for a discussion to only involve individual members, if you can't think 
    of appropriate mailing lists, you probably want to have a meeting instead.

* If there are any specific individuals you want to note as a FYI, add them as *cc:* addresses, not 
*to:* addresses.

* Email messages do not have to be long or formal - even a single-line email with a subject and 
no-text is just fine. The important aspect is whether an immediate reply or not is needed - even for 
short messages, if no immediate reply is needed email is more appropriate than chat.

* If you didn't get a reply in a reasonable amount of time, don't hesitate to ping the thread, with 
even just a one word "Ping", though it can be a little more polite to add some context like "Ping - 
any thoughts on XXX?"

### Email filters

Gmail supports advanced labeling and filtering mechanisms that can allow managing many different 
threads for various mailing lists in an efficient matter. While everyone will have their own 
preferences, there are some common practices that should be useful to everyone.

* Either use the "Show Indicators" setting in Gmail settings, or define a label named `me` and give 
it a prominent color (e.g., red). Define a filter that sets this label for all emails that satisfy 
`to:me`

* Define a label for each mailing list you are subscribed to, or at least important ones like the 
team list. Define filters to set the label for mail to each list.

* Define a label for jira and filter messages from jira@curioswitch.atlassian.net to it

* For github users, define a label for it and filter messages from notifications@github.com to it. 
Filter from:(github.com) to:(author@noreply.github.com OR review_requested@noreply.github.com OR 
assigned@noreply.github.com OR mention@noreply.github.com) to `me`

Generally, it is good to set label colors in order of importance to you.

An example configuration is like

Matches: from:(notifications@github.com)

Do this: Apply label "github"edit 

Matches: from:(jira@curioswitch.atlassian.net)

Do this: Apply label "jira"edit 

Matches: from:(-github.com) to:me

Do this: Apply label ":me"edit     

Matches: from:(github.com) to:(author@noreply.github.com OR review_requested@noreply.github.com OR 
assigned@noreply.github.com OR mention@noreply.github.com)

Do this: Apply label ":me"edit   

Matches: eng-all@curioswitch.org,

Do this: Apply label "eng-all"edit 

### Other tips

* You can see keyboard shortcuts in Gmail by hitting the question mark key `?` at any time, which 
can be very useful for processing threads.

* It's possible to mute email threads so they won't reappear in your inbox unless you are explicitly 
mentioned in `to:/cc:`.

* None of the above rules apply to non-work related communication, e.g., 1-1 with no mailing list is 
fine for non-work stuff.

## JIRA

JIRA is used to track tasks over time. JIRA is optimized for tracking a task which needs clear end 
status usually with pre-defined process which defines how and who to take action. For example, 
handling a bug in the CurioSwitch product is a good use case for JIRA since it needs well defined 
prioritization process and a clear goal status such as "Done", “Won’t fix” and etc. If your division receives an ask from another division as a part of its role, it is encouraged to create a JIRA project to manage the ask and to make a status of the task clear.

Example of good usage

* Track an ask from another team.

* File a bug of a product. JIRA is the only official place to receive a bug. It will be prioritized 
and processed by pre-defined handling process. 

    * There is no such thing as a wrong JIRA. Even if the behavior happens to be considered Working 
    as intended or Marked as duplicate, there is real value in knowing what points are confusing, or 
    what points should be prioritized.

* Track a new feature request, suggestion for operation improvement. A request/suggestion which 
doesn’t have to be solved now is easily missed. Long term tracking is a good fit for JIRA.

* Contain background/context as much as possible. JIRA tickets should contain sufficient context of 
the issue so readers of the ticket can have clear understanding of the ticket without asking for 
background of the report. 

* Periodically revisit existing tickets and re-prioritize tickets.

* Use filters. JIRA has filters by default. "My Open Issues" lists all issues that are assigned to 
you.

Examples of poor usage

* Discuss complicated issue on JIRA. If an issue needs a long conversation to share context or to 
come up with a solution, it should be discussed in a meeting with stakeholders and add a link of the 
meeting minutes should then be added to the ticket.

* Always set a high priority to your ticket. If you always set "P1" to your ticket, people won’t 
understand its actual urgency.

* Use a mention when it needs a reaction from someone. Mention is easily missed and make it unclear 
who is responsible for the next action. Change assignee to the person instead.

## Hangouts Chat

Hangouts Chat is ideal for conversations that need quick response time but has poor information 
management capabilities for dealing with large numbers of discussions. Hangouts Chat should only be 
used for times where a quick response is expected.

Example of good usage

* 1-1 chat to have someone walk you through a process

* Discuss a production outage, e.g. brainstorming solutions. Generally a production outage 
discussion within a team on Hangouts Chat should also be accompanied by an email sharing the status 
of the outage to a larger group

* 1-1 chat to send a link to the notes / slides that someone is about to use now, if they misplaced 
the link. Links to notes / slides etc for information sharing among groups should be done on email, 
not Hangouts Chat.

* Sharing articles on #random, the automatic snippet embedding makes it nice

* Sharing the location of the restaurant for a party everyone is getting ready to go to - this is an 
example of requiring immediate action.

Examples of poor usage (general theme is they don't require quick reaction)

* Sharing the link to a design doc, notes, etc. Either use Drive's sharing functionality or send an 
email.

* Making an announcement to a group, especially all employees.

## Meetings

Meetings are important for syncing up on a regular basis or having more involved discussions. 
However, it is also easy for meetings to take up more time for a small amount of output. A meeting 
that goes over the scheduled time has a large chance of producing no output at all. These guidelines 
aim to provide best practices to maximize the effectiveness of meetings and produce output. Meetings 
with no output have negative value, not zero or positive.

* Meetings must have corresponding notes in a Google Doc. Recurring meetings like a weekly team 
meeting should have a single notes document that gets updated every meeting. The notes document 
should be prepared prior to the meeting (ideally as soon as the meeting is scheduled) and placed in 
the Meeting Notes Team Drive folder. If there is an appropriate subfolder (e.g., team meeting), 
place it there, but if not, the top level is fine. Title of the document should be something like 
"[meeting notes] Engineering weekly" for consistency and searchability.

* A calendar entry should be added to schedule the meeting. Where possible, add mailing lists, not 
individual members. The calendar entry must have a link to its meeting notes and should have some 
context (what the meeting is about), especially for non-recurring ones.

    * It is basically never correct to add no mailing list to a recurring meeting - members tend to 
    change over time and using a mailing list ensures updates happen by managing members in the 
    single location, the mailing list.

* Unless prohibitive, an agenda should be prepared in advance and included at the start of the 
notes.

    * It is a good idea for the agenda, especially for non-recurring meetings, to clearly define 
    what output is expected from the meeting (e.g., make a decision on A, share the status of B). 
    Broad concepts like "discussion" tend to not be good output as they cannot really be evaluated.

    * If decision making is a part of the output, try to designate one or two (ideally one) 
    decision-makers who will have ownership. Without clear ownership of the decision-making, it's 
    common for much discussion to happen but end up with no one actually making the decision, and 
    therefore, no output.

    * It is highly encouraged to prepare at least a skeleton of an agenda! With no agenda, it is 
    common for meetings to drag on with little output, so even a lightweight agenda helps direct the 
    communication.

* If discussions get off-topic, and are not productive in achieving the agenda / desired output, 
interrupt them after a reasonable amount of time (e.g., 2 min) and schedule a follow-up meeting to 
go in-depth on that topic. It's common for such discussions to resolve even with just a couple more 
minutes among a subset of members after the meeting finishes. Record it as an action item in the 
meeting notes, so it won't be lost, and people feel assured that it will be revisited.

* Important decisions and Action Items should be clearly marked in the meeting notes, so it's easy 
for people to skim through. Use DN(maker) for an important decision made so people know who to 
follow up with, and AI(owner) tag to make clear who owns an action item, so it won't be left 
hanging, and it's straight-forward to convert it to a JIRA task. Also highlight the tag, add a 
comment, and use an @mention and the Google Docs assign feature so they have a reminder to act, even 
if just by creating the JIRA ticket.

* Meetings should start on time - if it is running late, strongly consider rescheduling the meeting 
rather than trying to finish in a shortened amount of time. Trying generally results in not 
achieving the desired output and requiring more time in the long-run.

    * Generally 30min or 60min are reasonable meeting times. Longer meetings tend to have too much 
    content planned, which increases the chance of going off-topic and usually results in less 
    efficient use of time. Unless there's a clear reason not to, try to split the meeting into 
    shorter chunks.

* After the meeting completes, copy the notes into an email addressed to your team's -minutes@ 
mailing list (described above) or meeting-minutes@. It is not necessary to clean up the notes, the 
goal is to share what happened in a way without too much overhead. The email title should be the 
same as that of the notes for consistency, and it should have a link to the original notes document 
at the header, so in case audience wants to comment on a particular section of the notes, it's easy 
to do so.

More details on concrete steps that can improve meeting efficiency are at the 
[Meeting Guidelines](../meeting).

## Drive

All file storage at CurioSwitch is in Google Drive - Google Drive provides extensive collaboration 
features to accelerate our general activities and provide transparent, effective communication. 

The primary block of Google Drive is a Team Drive which provides access to all the documents inside 
to specified members (by default it provides access to only the person that created the Team Drive). 
We use mailing lists to manage access to team drives to ensure access is updated with team 
membership changes automatically. These drives should be used for the convenience of the members, 
but here are some guidelines to more effectively use them.

* We aim for transparency throughout the company - generally try to provide more access than less 
unless the documents are clearly sensitive.

* The All team drive is a full-access drive that all full-time employees can access - it's 
membership is essentially "Full access to all-fte@". Anytime you want to give full access to a 
document (for example, presentation slides given to the entire company, meeting minutes, etc), this 
drive is an appropriate location for it.

* It is appropriate to have separate team drives for teams or other purposes, even with fully 
visible content. The two reasons for this is to 1) minimize accidental edits and 2) allow access to 
team-specific contractors. The recommended membership for such a drive is

    * Team FTE mailing list (e.g., sales-fte@) with Full access

    * all-fte@ with Comment access

        * Comment access still provides view access to all documents. The reason to restrict to 
        comment access is not to control access but to prevent accidental edits. Especially when the 
        company gets huge, the more people with the ability to edit, the greater chance there is of 
        a completely accidental edit. Documents can still be shared with edit access to others on an 
        individual basis as needed.

    * Team TVC mailing list or individual TVCs with appropriate access based on team needs

* While we encourage transparency, material like hiring and performance reviews must be kept 
confidential. For sensitive drives, only give Full access to a mailing list with members that have 
access to this information, and no other members.

* Team drives should never have membership that is only individual members. Always add a mailing 
list, creating one as needed for unique groupings.

* Team drives should never have members that are part of a mailing list that is already a member - 
it means the mailing list is not the source of truth for membership. Individual membership is 
generally for allowing one-off membership of external members like TVC.

Some other general guidelines are

* Drive supports templates, which can be used for creating consistency and convenience when creating 
somewhat standard documents. Don't be shy, if you have a document that seems useful to others, 
create a template.

    * The general flow for submitting a template is

1) Make a copy of an example doc to My Drive

2) Replace real content with explanations of what goes in each section

3) Go to the sharing settings > advanced and make sure "Disable options to download, print, and copy 
for commenters and viewers" is not checked

3) Go to New > ... > From a template for the doc type you are submitting.

4) Press Submit a Template

5) Locate your document by searching for its name

6) Check "Submit a copy of the file" to make sure the template isn't loss if i.e., the submitter 
leaves the company

* Drive search is by Google, so it works great. When locating documents, you can generally search 
for the title or a keyword you remember about it and you'll find it. Browsing through the drive 
hierarchy is for many users a rare occurrence.

* All the document types support Insert > Bookmark pretty much anywhere. Take advantage of this to 
link to actual words in a document / spreadsheet when appropriate.

* Avoid downloading documents. Drive is meant to be our ground truth store of long-form information 
at CurioSwitch - downloading documents increases the security risk around this information and 
should only be done when absolutely required (e.g., sharing to an external party).

* Documents can be moved between team drives freely. It is completely natural to not want to share a 
document with others until it is ready - for such cases, you can create the document in My Drive at 
first and then move it to the appropriate team drive when comfortable.

* To request review of a document, always use the Share button to share to members or mailing lists 
you want to have review it with some context. This will rarely add new access permissions to the 
document, but still allows us to have a standard notification for requesting reviews - Gmail will 
render such notifications specially too.

    * If you'd like explicit sign-off on a document, add a Sign-off table at the end with desired 
    reviewers, and add a comment to their name with for example +choko@curioswitch.org to notify 
    them.
