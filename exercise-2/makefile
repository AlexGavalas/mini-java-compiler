all: compile

compile:
	java -jar ./jtb132di.jar minijava.jj
	java -jar ./javacc5.jar minijava-jtb.jj

compileMain:
	javac Main.java

execute1:
	java Main ./Examples/No/BadAssign.java ./Examples/No/BadAssign2.java ./Examples/No/Classes-error.java ./Examples/No/DoubleDeclaration1.java ./Examples/No/DoubleDeclaration4.java ./Examples/No/DoubleDeclaration6.java ./Examples/No/NoMatchingMethod.java ./Examples/No/NoMethod.java ./Examples/No/Overload1.java ./Examples/No/test18.java ./Examples/No/test21.java ./Examples/No/test35.java ./Examples/No/test52.java ./Examples/No/test68.java ./Examples/No/UseArgs.java

execute2:
	java Main ./Examples/Yes/Add.java ./Examples/Yes/ArrayTest.java ./Examples/Yes/CallFromSuper.java ./Examples/Yes/Classes.java ./Examples/Yes/DerivedCall.java ./Examples/Yes/Example1.java ./Examples/Yes/FieldAndClassConflict.java ./Examples/Yes/Main.java ./Examples/Yes/ManyClasses.java ./Examples/Yes/OutOfBounds1.java ./Examples/Yes/Overload2.java ./Examples/Yes/ShadowBaseField.java ./Examples/Yes/ShadowField.java ./Examples/Yes/test06.java ./Examples/Yes/test07.java ./Examples/Yes/test15.java ./Examples/Yes/test17.java ./Examples/Yes/test20.java ./Examples/Yes/test62.java ./Examples/Yes/test73.java ./Examples/Yes/test82.java ./Examples/Yes/test93.java ./Examples/Yes/test99.java	

execute3:
	java Main ./Examples/General/BinaryTree.java ./Examples/General/BubbleSort.java ./Examples/General/Factorial.java ./Examples/General/LinearSearch.java ./Examples/General/LinkedList.java ./Examples/General/MoreThan4.java ./Examples/General/QuickSort.java ./Examples/General/TreeVisitor.java 

execute4:
	java Main ./Examples/General/BubbleSort-error.java ./Examples/General/Factorial-error.java ./Examples/General/LinearSearch-error.java ./Examples/General/LinkedList-error.java ./Examples/General/MoreThan4-error.java ./Examples/General/QuickSort-error.java ./Examples/General/TreeVisitor-error.java 

clean:
	rm -f *.class *~
