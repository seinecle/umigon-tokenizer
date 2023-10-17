# Umigon
A family of modules for essential NLP tasks and sentiment analysis, done well.

## The umigon-tokenizer
Why another tokenizer?? **Because splitting on whitespaces is not enough.**

## Installation

```xml
<dependency>
	<groupId>net.clementlevallois.functions</groupId>
	<artifcactId>umigon-tokenizer</artifactId>
	<version>0.20</version>
</dependency>
```
Or [check on Maven](https://central.sonatype.com/artifact/net.clementlevallois.functions/umigon-tokenizer) to see the latest version.


## Releases

* 2023, Oct 17: version 0.20

Upgraded the emoji dep to 1.3.0. Fixed a part where looking for the alias of an emoji could return a NPE.

* 2023, Oct 10: version 0.19

Removed the throwing of the IO exception. Updated tests dependencies.


* 2023, Aug 28: version 0.18

Updated the deps so that we have a cleaner separation of the model for texts, not text classification.


* 2023, Aug 25: version 0.17

Replaced the lib for emojis for a new one without vulnerability and maintained. Added tests.


* 2023, April 13: version 0.14

Added a static method initialize() to allow the reading of the emoji file to be performed just once at the initialization phase and not each time the tokenizer is called. Made change to the Pattern Of Interest Checker to make it concurrent.

* 2023, April 13: version 0.13

Adds slashes and pipes ("/" "\" "|") as punctuation signs that separate a string of characters in 2 different tokens. Before that, only hyphens had the effect to split strings of characters.

Apostroph signs continue NOT to split a string of chars. So "can't" will make one token ("can't"), but "I would think/believe that" would now be tokenized into "I", "would", "think", "/", "believe", "that".

* 2023, March 28: version 0.11

Fixes a critical issue. Resource files are moved to a resource folder, where they belong.


* 2023, March 24: version 0.10

Initial release


## Usage
```java
String text = "I can't *wait*  to see this performance! 𝄠\nI will l@@@ve it :-) 😀😀😀 😀 :((( ";

Set<String> languageSpecificLexicon = new HashSet();
// this set is for the following purpose:
// if the text to tokenize includes words such as "yeeees", you can provide a Set of Strings containing the word "yes". The tokenizer will make sure to store, for the token "yeeees", both the original form "yeeeees" and the cleaned form "yes".

UmigonTokenizer controller = new UmigonTokenizer();
List<TextFragment> textFragments = UmigonTokenizer.tokenize(text, languageSpecificLexicon);
String beautiffiedPrint = controller.printTextFragments(textFragments);
System.out.println(beautiffiedPrint);
```


## Example

Consider this sentence:

> I can't *wait*  to see this performance! 𝄠
> I will l@@@ve it :-) 😀😀😀 😀 :((( 

Will be tokenized as:

text fragment: I (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: can (type: TERM)

text fragment: ' (type: PUNCTUATION)

text fragment: t (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: * (type: PUNCTUATION)

text fragment: wait (type: TERM)

text fragment: * (type: PUNCTUATION)

text fragment:    (type: WHITE_SPACE)

text fragment: to (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: see (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: this (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: performance (type: TERM)

text fragment: ! (type: PUNCTUATION)

text fragment:   (type: WHITE_SPACE)

text fragment: 𝄠 (type: TERM)

text fragment:  (type: WHITE_SPACE)

text fragment: I (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: will (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: l@@@ve (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: it (type: TERM)

text fragment:   (type: WHITE_SPACE)

text fragment: :-) (type: EMOTICON_IN_ASCII)

text fragment:   (type: WHITE_SPACE)

text fragment: 😀 (type: EMOJI)

text fragment: 😀 (type: EMOJI)

text fragment: 😀 (type: EMOJI)

text fragment:   (type: WHITE_SPACE)

text fragment: 😀 (type: EMOJI)

text fragment:   (type: WHITE_SPACE)

text fragment: :((( (type: EMOTICON_IN_ASCII)

text fragment:   (type: WHITE_SPACE)


## Principles followed
- the least dependencies possible:
   * a Utils module which has no further dependency
   * the emoji-java library, which has a single dependency on org.json
   * the umigon-model module which is the dependency-free data structure for the Umigon modules

- each module should serve a function in a stand-alone fashion: you can use it independently from other modules.


## How to cite it
"Clement Levallois. 2013. Umigon: sentiment analysis for tweets based on terms lists and heuristics. In Second Joint Conference on Lexical and Computational Semantics (*SEM), Volume 2: Proceedings of the Seventh International Workshop on Semantic Evaluation (SemEval 2013), pages 414–417, Atlanta, Georgia, USA. Association for Computational Linguistics." 

## Web app
The tokenizer and all Umigon modules are implemented in [Nocodefunctions](https://nocodefunctions.com), a free web app for nocode data analysis.

## Contact
[Clément Levallois](https://twitter.com/seinecle)

## License
See [license.md](LICENSE.md)