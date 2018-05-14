# ExactLearner
Backend engine for ExactLearner (without graphical user interface)

From running the jar file, the following array elements specify the parameters for the learner

[0] ontology path
 
LEARNER SKILLS:
- [1] = decompose left [t/f]
- [2] = branch left [t/f]
- [3] = unsaturate left [t/f]
- [4] = decompose right [t/f]
- [5] = merge right [t/f]
- [6] = saturate right [t/f]

ORACLE SKILLS:
- [7] = merge left [0..1]
- [8] = saturate left [0..1]
- [9] = branch right [0..1]
- [10] = unsaturate right [0..1]
- [11] = compose left [0..1]
- [12] = compose right [0..1]


----- OUTPUT 

aside from some console metrics (number of equivalence queries
and some other info) a new ontology file will be created in the folder of
ontology input this new ontology will be the hypothesis learned by the
program

		
java -jar ExactLearner.jar animals.owl t t t t t t 0 0 0 0 0 0 


@Param
ontology to load = animals.owl 
learner skills (see above for details) = t t t t t t 
oracle skills (see above for details) = 0 0 0 0 0 0 

