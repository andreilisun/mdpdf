# mdpdf

Kotlin script that retrieves PDF annotations and saves them as Markdown file. Currently supports highlight and popup annotations only.

## How to use

1. Install Kotlin ([How to install](https://kotlinlang.org/docs/tutorials/command-line.html)).

2. Run *mdpdf* script.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Directly from Github:

```
kotlinc -script https://raw.githubusercontent.com/andreilisun/mdpdf/master/mdpdf.main.kts [path to pdf file] [save location(.md file)]
```

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Download script and run locally:

```
kotlinc -script mdpdf.main.kts [path to pdf file] [save location(.md file)]
```

## License
```
Copyright 2020 Andrii Lisun
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```