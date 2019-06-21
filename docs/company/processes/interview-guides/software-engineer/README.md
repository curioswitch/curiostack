# SWE Interview Guide

This document outlines core guidelines and process for conducting software engineering job
interviews during the hiring process. A strong team can only be created with a strong hiring process
supported by a well-defined and calibrated interview process. While this document will describe
software engineering interviews in detail, many of the concepts should apply to other functions too.

Any text in quotes ("") is example language and should not be memorized - use them as a guide to
provide your own personal tone to language used in the process.


## Introduction

All candidates for positions at CurioSwitch are given role-specific interviews to determine whether
we should give them the job. While interviews will always produce many insights into a candidate, we
are generally looking at two broad areas

*   Role-specific qualifications. This reflects how well the candidate would be able to perform
their day-to-day job activities. For software engineer, this includes computer science knowledge,
programming skill, software system design, and technical leadership.

*   Value fit. This reflects how well the candidate aligns with CurioSwitch's values and culture.
Candidates may be able to do a perfect job in their role as a single member. However, we believe
that to achieve a great company means everyone having aligned values, which naturally leads to
better inter-personal communication and team interactions and use the interviews to understand how
well the candidate aligns with our values.

In addition to these, there are principles that we keep in mind during all interviews and these
guidelines are intended to assist in achieving them.

*   Fair assessment. We want to understand the candidate in respect to the above two areas as best
as possible, in an unbiased and fair way.

*   Respect privacy. A candidate is entrusting us with a very personal point, that they are
considering changing jobs. We must give upmost respect to their privacy in exchange for this trust.

*   Find the candidate's strengths. The interview is not meant to point out a candidate's
weaknesses, but should only focus on finding their strengths so we can understand how well they
match our expectations. 

*   Give a good impression of CurioSwitch. Every interview is a two-way assessment; just as we are
evaluating the candidate for how good a fit they would be for us, the candidate is assessing our
company as an employer. Even if a candidate is not the best candidate for the current role, we want
them to apply for other roles if they are suitable, or tell friends, colleagues, etc. about how much
they enjoyed interviewing with us.


## Preparing

The first step in conducting an interview will always be when a recruiter assigns you to do an
interview for a candidate. This is currently handled completely through Gmail and calendar - a
calendar entry will be added for the interview, and an email will be sent with the request for
interview and a link to their resume on Drive. As the company gets bigger, we will transition to
using Google Hire as our hiring tool, which works best thanks to its awesome G Suite integration.

Before anything else, check the timing of the invitation - if the schedule does not work for you,
decline the invitation so the recruiter knows they must reschedule or find a different interviewer.
Depending on the schedule of the candidate, you may not actually end up doing the interview if it's
more appropriate to assign a different one.

Otherwise, accept the invitation. In order to understand the candidate to conduct a smooth
interview, you will need to go through their resume. When this is comfortable will vary by
interviewer, but it's common to go through it once on the day before the interview, and then skim
through once more just before the interview. Things that are often important to pick out include

*   What technology has the candidate used before?
*   How many years of work experience they have, in what roles?
*   Does the candidate seem to lean towards development roles or management roles?
*   Does the candidate seem to lean towards low-level development (e.g., OS kernels) or high-level
development (e.g., Rails application)?

While getting to understand the skills and tendencies of the candidate, come up with a pool of
questions to ask the candidate during the interview. This should include both small questions to
clarify details from the resume to understand them better as well as the actual role-specific
questions you will ask. As multiple interviews will interview the same candidate, there is a chance
a question you thought of will have been asked by someone else in a session right before yours;
therefore, it is important to have a pool of questions that you would draw from. For software
engineering interviews, it is generally good to target asking two technical questions, or one large
question, and have a pool of 3-4 prepared.


## Coming up with questions

There is no such thing as a perfect interview question - interviews are conversations between an
interviewer and a candidate and the questions tend to reflect the personal style of the interviewer.
It's recommended for interviewers to come up with interview questions that they can relate with,
each with a particular weight towards one or more of the following areas

*   Algorithms + computer science
*   Coding
*   Open-ended problem solving
*   System Design

Throughout the slate of interviews, we would like to have some coverage of each of these areas. It
is common for your questions to have a focus on one of these areas, and the two questions provide
coverage of 2/4, but there can be multiple areas addressed by a single question and the coverage is
not guaranteed to be full or uniform, but we try.

It is good to come up with questions that are particularly interesting to you which you will enjoy
giving during the interviews. But as you get used to interviews in the early stages or want to see
if anyone is asking anything new and interesting, feel free to look at our database of questions.
It is perfectly fine to use these questions as is too - the main reason for the recommendation of
creating your own questions is that it is common to eventually find questions to get boring after
asking them so many times. It's good to change up your repertoire whenever you feel this happening.
It's hard to be interested in a candidate's answer if you are already bored of the problem itself.

It is good to avoid trivia questions (fermi's theorem, arcane detail of the JVM, etc). Some are not
related to actual job performance at all, and others are just knowledge, whether the candidate has
happened to run into it in their career or not. **We are more interested in evaluating their
engineering skill, not knowledge**. If the resume mentions being an expert on something, like JVM,
it can still be ok to ask a little about it. But it's better to form a problem-solving question
(Instead of "How does the JVM do JIT", something like "How would you make a Java program run as
fast as possible?", the latter will by nature address the JIT too.

In addition to the four focus areas above, try to think of the questions in terms of work experience
levels too

*   Junior
    *   Not necessarily have as much experience in real-world development
    *   Questions should focus on coding, algorithms, CS and can lean towards the simpler problems

*   Middle (also called upper-junior)
    *   Fair experience with real-world development but not necessarily leading complex projects
    *   Questions can cover all areas, with coding questions being more complex than purely junior
    candidates

*   Senior
    *   Should have experience with all areas of real-world development
    *   Questions should cover all areas, with some focus on systems design and open ended questions

In general, junior candidates will be comfortable coding but don't have so much system design or
high-level thinking experience yet. Asking questions with a larger focus on coding is appropriate.
On the flip side, senior candidates will have unique strengths related to more open-ended and system
design related aspects and should be probed on those. They are still expected to have basic skills
like strong coding and algorithms knowledge, though.

After you've gone through the candidate's resume and have an idea of what their general work
experience level is and their areas of expertise, come up with the question pool taking into the
above while also trying your best to match it to the candidate's unique experience. For example, if
the candidate has worked at a bank, it is good to frame a question in the context of security. Or if
the candidate is experienced with machine learning, it is good to ask to solve some sort of
recognition task. This will be difficult in the early interviews, and it's fine. But as you get more
experience you will probably find that you can come up with questions that the candidate will be
more comfortable with. To reiterate one of the principles, the point of the interview questions is
to find the candidate's strengths. It is easier to find their strengths if we can try to match
questions to the skills and experience listed on the resume.

### Illegal questions

We put a strong emphasis on protecting the privacy of the candidate and conducting unbiased
interviews. Because of this, it is important we do not ask questions that would give us too much
information about the candidate, or that is not related to the actual role. We want to make sure to
evaluate a candidate based on their strengths, and will make efforts to accommodate many different
lifestyles for new hires with varying needs - job interviews are not the tool to accomplish this. 

Things that an interviewer must not ask include

*   Age
*   Race, ethnicity, nationality
*   Gender
*   Religion
*   Disabilities
*   Marital, family status
*   Salary

You will naturally form some assumptions about some of these points when reading the candidate's
resume - try your best not to, and definitely don't ask questions related to them. Remember,
questions can easily indirectly give input about these points and are not allowed. For example,

*   When did you graduate from high school? (Age)
*   What does your wife do for a living (Family status)
*   Will you need personal time off for particular holidays? (Religion)
*   We have a pretty young team, how does that make you feel? (Age)
*   There are two years outside of a company on your resume, what were you doing then? 
(Disability / family status)
*   Are you okay with less cash in exchange for options? (Salary, family status)

It may seem like some of these are points that will need to be taken into account at some point - it
is people operations team's job to do so, not interviewers.

It is fairly common for one of these topics to come up spontaneously from the candidate, especially
in the last few minutes of the interview. Even then, try to avoid going further on the topic. For
example, if a candidate asks "How is maternity leave at the company?", you can answer "XX months" or
"Actually, I'm not sure you should check with the recruiter about that". But do not go further, with
anything like "Oh, are you expecting kids?", etc.


## Conducting the interview

The interview is primarily a conversation between you and the candidate, however they will mostly
follow a pattern that looks like this

1. Greet the candidate and introduce yourself briefly (don't have to provide much more detail than 
"e.g., I'm an engineer at CurioSwitch").

2. Give an outline of the flow of the interview. Something like "I'm going to start with a question
or two to get to know you better, and then go into a couple of technical questions, including coding
on the whiteboard, and will leave you 5 minutes at the end to ask me questions." Also it's good to
point out e.g., "The interview is to find your unique skills and strengths, and it's normal to not
to know enough about every question I ask, in which case we may quickly move onto a different
question."

3. Ask an introductory question or two. This is often something related to their resume (e.g.,
"Were there any significant challenges in the XXX project") or if it seems like other interviewers
have already dove well into the resume, can be more general (e.g., "What is the most challenging
issue you have ever run into in software engineering?").

    - If you are the later interviewer in a series of them, you can consider shortening this, and
    instead ask if the candidate can use a restroom break, or water, etc.

4. Ask a technical question. Unless you are only planning on asking one deep question in the
interview, it is good to go in order of complexity to give the candidate time to warm up. It is
common for the first technical question to be a coding question.

5. Ask another technical question if you have time (you may have planned two questions and even then
not be able to ask another one if they took up all time on the first). It is common for this
question to be more open ended or about system design.

6. When there are around 5 minutes left, find a good point to close the questions (may need to
interrupt a line of answering). Give them an opportunity to ask you questions about the company
e.g., "Do you have any questions for me, maybe about the company or work style?" You can clean the
whiteboard while they are talking (take a photo first if it will help you later).

    - Be relatively open when answering questions about the current state of the company, including 
    explaining the technical stack in relative detail if asked. 
    - Avoid talking about future plans / roadmaps, and also only answer questions you are familiar 
    with without speculation. Feel free to say e.g., "Sorry, I'm not too sure about that but we will 
    get back to you on that."

7. Thank the candidate for taking the time. If you are their last interviewer for the day, escort
them to the elevator hall and see them out.

You should be taking notes during the interview to recall later when writing feedback about the
candidate. It is recommended to use paper notes, since using a laptop can make candidates feel like
the interviewer isn't paying attention (doing some other work).

Things to keep in mind during the interview

*   Be friendly from beginning to end of the interview. Even if a candidate's performance seems
weak, we want them to have an enjoyable experience and leave without a negative impression of
CurioSwitch.

    *   Some candidates may really have no ability to perform their role, and for example can't code
    at all. Try your best to be pleasant and come up with filler questions to reach the end of the
    interview. Later, feel free to vent frustration to your manager or the recruiter to allow
    calibrating the sourcing pipeline.

*   Never go over time. Our current interview session is set at 1 hour - do not use more than 1 hour
in your interview even if your meeting room is still open after that. It is not possible to fairly
compare two different interview results if the lengths are different.

*   Try to end close to the scheduled end time. Rather than ending early after two questions, if
there's still plenty of time ask another, possibly filler question. Candidates will be nervous if
the interview ends significantly earlier than expected.

*   Allow the candidate to use any programming language they are familiar with for coding. We
consider strong coders to be language-agnostic and would pick up on our tech quickly. However, if
the resume makes it obvious they are, e.g. an expert on Java feel free to ask to code in it from the
start.

*   Not all candidates are native English speakers. Give non-native speakers more time to form their
thoughts when discussing, and try to avoid metaphors or expressions that may confuse the candidate.
    
    *   Interviews don't have to be conducted in English. If you're comfortable speaking in the
    candidate's native language in the context of a job interview, feel free to use it. 

*   Don't let candidates get stuck. We want to find the candidate's strengths, and if a candidate is
stuck and stops making progress, we stop being able to do that. Give them some time to think
problems through, but give hints at appropriate times too to keep them moving through the problem,
often by giving a more vague hint and following up with more obvious ones.
    
    *   There is no good rule for when to hint - you will get used to it as you give more interviews
    and understand how long candidates take to go through certain parts of the problem. Pace hints
    to steer the candidate to at least come up with a basic answer to the problem in the time you
    expect for the question. Another way to think about it is to aim to ask all the questions you
    had planned.
    
    *   There will be some times when you just have to give what's basically the answer. Try your
    best to add some vagueness or something to make it seem like you aren't though, or candidates
    will get nervous knowing they didn't answer the question.

*   If a candidate doesn't seem to get started on a question, due to a lack of understanding of the
concept or even something else, move onto another question, maybe with something like "I guess you
haven't run into this sort of thing before, let's actually go to a different topic.". Like the above
point, don't ever leave a candidate stuck on a question.

*   Encourage the candidate to speak their thoughts. While a candidate is thinking to themselves,
it is hard to know if they're stuck or not, and it can also be hard to know where they are when
determining hints. If they've been silent for a while, get them talking e.g., "What's on your mind?"
or "If you have any thoughts, happy to hear them."

*   Especially for coding questions, encourage the candidate to speak through their solution before
jumping into code. It's common for their first thoughts to be on a wrong track, and if they jump
into code with that in mind, it is very easy to get stuck. Instead, try to make sure the candidate
at least has a general idea of the solution before writing code and help them get to it if they have
problems when speaking through the problem.

*   Sometimes a candidate may ask e.g., "How did I do on that problem?". You must not answer this
during the interview, and you can just honestly answer with something like "Sorry, I can't talk
about the performance on a question during the interview."

*   All of the company's rules and values apply - treat the candidate at least as well as you would
treat any of your peers.

## Writing interview feedback

The final step is for you to write feedback about the interview. Our current process is for you to
send an email directly addressed to the recruiter with a subject like
"[Interview Feedback] Candidate Name", though at a later stage we will use Google Hire for entering
feedback. There are four parts to interview feedback.

*   Score. We assign a whole number score to all candidates from 1-5
    *   The middle line is 3 - this means you have no impression on whether we should hire or not.
    Try your best to avoid giving a score of 3 unless this is really how you feel given the
    interview answers. 
    *   Above 3 is a ok to hire with varying confidence. Generally, a score of 4 indicates leaning
    towards hire but not so confident and other data points (e.g., other interviews) are needed or
    may convince otherwise. Scores of 5 are strongly confident in hiring.
    *   Below 3 is a not ok to hire, with varying confidence. A score of 2 usually means leaning
    towards no hire but if other data points show strength could reconsider. 1 is always a strong
    recommendation to not hire. Keep in mind it is very rare to hire a candidate with even one score
    of 1 as it indicates a red flag, and many scores of 1 would probably indicate we don't ever
    revisit the candidate in the future.
    *   Do not think too deeply over how to give this score. The hiring process is designed to take
    into account different calibrations among interviewers, so it is enough to follow these
    guidelines in a general way.

*   Summary: Provide a paragraph (3-5 sentences) of text explaining your hiring recommendation.
Usually starts with a sentence similar to "Recommend to hire", or "Lean towards no hire, but could
be convinced otherwise if their XXX was great in other interviews". Then provide some high level
comments on the candidate supporting this recommendation. Point out particular strengths, particular
weaknesses, and your general sense on the candidate.

*   Questions. This is the longest section of interview feedback. Go through every question you
asked the candidate in order describing the question and answer. Err on more information than less,
the hiring committee will look at the content to come up with their own evaluation of the candidate
along with yours.

    *   First write the question you asked (doesn't have to be the exact words as long as what
    question it was is obvious). If you created the question from scratch yourself, it is also
    helpful if you can link to a document describing it like in our interviewing questions database.
    *   Go through the candidate's answer. For coding, it should include the actual code, including
    any significant revisions. It is very helpful to also include some points on the candidate's
    thought process (e.g., "I gave a hint, and the candidate stopped to think a minute on it but
    then he quickly was able to figure out the best answer to the question.").
    *   For non-coding questions, it's good to go into more details on the statements that candidate
    made.
    *   After documenting the candidate's answer, provide a sentence or two on your evaluation of
    the answer, e.g., "This code is syntactically correct and idiomatic", "The candidate had a lot
    of trouble putting their thoughts together, and I didn't really understand the answer."

*   Value fit: Add a sentence or paragraph on how well you think the candidate fits with the
company's values. In particular, it's important to express how much you would like to work with the
candidate ignoring their technical performance. This doesn't have to be a detailed analysis vs the
defined values, but a more general statement with a couple of examples from the candidate's
interactions.

Note, for strong recommendations not to hire a candidate, it is ok to leave out details. It can take
a long time to write interview feedback and while more detail is always better, for these candidates
it is not as important.

For borderline candidates, we sometimes only do one screening interview and have follow-ups depend
on the performance in the first one. If you are doing one of these, you should add an Interview
recommendation at the beginning on whether you think we should move on to additional interviews.
This is independent of, though naturally tangentially related to, your decision on whether they
should be hired or not.

## Shadowing

You are probably reading this document since you are about to start giving interviews. First-time
software engineering interviewers start by doing shadow interviews. In shadow interviews, you
conduct an interview with another, experienced interviewer. There are generally three shadow
interviews:

*   Full-shadow. You join the interview but only listen and watch, without asking any questions.
*   Half-shadow. You join the interview, and mostly listen and watch and let the other interviewer
drive the session. But they will give you a chance to ask a question and go through it with the
candidate.
*   Reverse-shadow. You drive the interview and ask all the questions, with the experienced
interviewer only listening and watching. They'll help with any issues that may come up though.

While the minimum expected shadow interviews is one of each type, you can have more than one of any
given type until you feel you are comfortable to move on. Moving on from reverse shadow means giving
full interviews by yourself.

After a shadow interview, do not talk to the other interviewer about the interview at all. Give your
feedback to the recruiter as described above, and then schedule a 30-minute session with the other
interviewer to discuss each others feedbacks. You and the other interviewer will both pull up your
feedback and the experienced interviewer will provide any comments and suggestions they have. Feel
free to casually use this session to understand interviews better - you will probably have concrete
questions based on what you saw in the interview.
