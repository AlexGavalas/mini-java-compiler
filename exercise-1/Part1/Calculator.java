import java.io.InputStream;
import java.io.IOException;

public class Calculator {

    private InputStream input;
    private int lookaheadToken;

    private Calculator(InputStream input) throws IOException {
        this.input = input;
        lookaheadToken = input.read();
    }

    private double factor() throws IOException, ParseError {
        if(lookaheadToken >= '0' && lookaheadToken <= '9') {
            double num = Double.parseDouble(Character.toString((char) lookaheadToken));
            lookaheadToken = input.read();
            return num;
        }
        else if(lookaheadToken == '(') {
            lookaheadToken = input.read();
            double num = expression();
            lookaheadToken = input.read();
            return num;
        }
	else throw new ParseError();
    }

    private double restTerm(double num) throws IOException, ParseError {
        if(lookaheadToken == '*') {
            lookaheadToken = input.read();
            double num2 = factor();
            double res = num2 * num;
            System.out.println("Result is " + res);
            restTerm(res);
            return res;
        } else if(lookaheadToken == '/') {
            lookaheadToken = input.read();
            double num2 = factor();
            double res = num / num2;
            System.out.println("Result is " + res);
            restTerm(res);
            return res;
        }
        else return num;
    }

    private double term() throws IOException, ParseError {
        double num = factor();
        return restTerm(num);
    }

    private double restExpression(double num) throws IOException, ParseError {
        if(lookaheadToken == '+') {
            lookaheadToken = input.read();
            double num2 = term();
            double res = num + num2;
            System.out.println("Result is " + res);
            restExpression(res);
            return res;
        } else if(lookaheadToken == '-') {
            lookaheadToken = input.read();
            double num2 = term();
            double res = num - num2;
            System.out.println("Result is " + res);
            restExpression(res);
            return res;
        }
        else return num;
    }

    private double expression() throws IOException, ParseError {
        double num = term();
        return restExpression(num);
    }

    private void goal() throws IOException, ParseError { expression(); }

    public static void main(String[] args) {
        try {
	    System.out.println("Type your arithmetic expression:");
            Calculator calc = new Calculator(System.in);
            calc.goal();

        } catch (IOException | ParseError exception) {
            System.err.println(exception.getMessage());
        }
    }
}
