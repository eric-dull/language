# Introduction

Language standalone application designed to be a text analysis service. 
It is based on the Stanford NLP library (http://nlp.stanford.edu/).
It provides Named Entities Recognitions functions, receiving a text as input, and returning entities of following types :

* PERSON 
* ORGANIZATION
* LOCATION

# Min. Requirements 

* `Java 1.8.0_65` 
* `Scala 2.11.7`
* `SBT 0.13.9`

# Installation 

* Clone the repo
* Run `sbt run` in the repo folder

# API (instable : changing!)

## Post Requests 

Important: please not that language param is not used at the moment

Every POST request can either :

* provide the submitted text in the body of the request, and the language in the URL : `lang=en`
* or provide a JSON object like follows by specifing `input=json` in the URL

  ```
    { 
        "text": "My text content",
        "lang": "en"
    }
  ```
 
## Routes and methods 

* `POST /entities/list` : returns the list of entities with their position as a JSON object
* `POST /entities/positions_by_type` : returns the list of entities grouped by type and text, and a list of every position for a given entity
* `POST /entities/highlighted_text` : returns and HTML document with highlighted entities according to their type
 

