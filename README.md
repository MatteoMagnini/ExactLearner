# ZhaOWL_ELLearner_NoUI
ZhaOWL version without user interface
Should improve execution in console

From running the jar file, the following array elements specify the parameters for the learner

[0] ontology path
if jar file is in project folder [ZhaOWL]
then you specify the small ontology for animals as
src/main/resources/ontologies/SMALL/animals.owl

[1] = mode, if "on" then easy mode is selected which overrides oracle skills
if "off" then normal mode AND allows for oracle
 
LEARNER SKILLS
[2] = decompose left 
[3] = branch left 
[4] = unsaturate left 
[5] = decompose right 
[6] = merge right 
[7] = saturate right

ORACLE SKILLS
[8] = merge left 
[9] = saturate left 
[10] = branch right 
[11] = unsaturate right


----- OUTPUT aside from some console metrics (number of equivalence queries
and some other info) a new ontology file will be created in the folder of
ontology input this new ontology will be the hypothesis learned by the
program

if we want to run easy mode, animals ontology, all learner skills and no oracle skills
		
java -jar consoleLearner.jar src/main/resources/ontologies/SMALL/animals.owl on t t t t t t f f f f
@Param
ontology to load = src/main/resources/ontologies/SMALL/animals.owl 
easy mode = on //// this overrides any oracle skills
learner skills (see above for details) = t t t t t t 
oracle skills (see above for details) = f f f f

