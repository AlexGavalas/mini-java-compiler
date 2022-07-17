import syntaxtree.Goal;
import java.util.HashMap;
import java.io.*;

class Main {
    
    public static void main (String [] args) {
		if(args.length == 0) {
		    System.err.println("Usage: Main <file1> <file2> ... <fileN>");
		    System.exit(1);
		}

		for(int i = 0 ; i < args.length ; i++) {
			
			FileInputStream fis = null;
			ClassInfo main = new ClassInfo();
			main.name = "GLOBAL";

			try {
			   
			    fis = new FileInputStream(args[i]);
			    MiniJavaParser parser = new MiniJavaParser(fis);
			    //System.out.println("Program parsed successfully.");
			    FirstVisitor eval = new FirstVisitor();
			    Goal root = parser.Goal();

			    if(root.accept(eval, main).equals("OK")) {
			    	//System.out.println(args[i]);
					SecondVisitor eval2 = new SecondVisitor();
					
					if(root.accept(eval2, main).equals("OK")) {
						//System.out.println("Program OK");
						//main.printOffset(main);

						ThirdVisitor eval3 = new ThirdVisitor(args[i]);
						root.accept(eval3, main);
						System.out.println("All done!");
					}
					else System.out.println("Type Error");
				
				} else System.out.println("Type Error");
			}

			catch(ParseException ex) {
			    System.out.println(ex.getMessage());
			}

			catch(FileNotFoundException ex) {
			    System.err.println(ex.getMessage());
			}
			finally {
			    try {
					if(fis != null) fis.close();
			    }
			    catch(IOException ex) {
					System.err.println(ex.getMessage());
			    }
			}
		}
    }
}