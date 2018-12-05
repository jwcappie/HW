import java.util.Stack;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.lang.Exception;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.HashMap; 
import java.util.Map; 

class StackElement<T>{
		T val;
		public void writeToFile(BufferedWriter bw){
			try{		
				bw.write(this.toString()+"\n");
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		public T getValue(){return val;}
}
interface Evaluable{
	public StackElement eval(Configuration c)throws Exception;
}
class UnitStackElement extends StackElement<String> implements Evaluable{
	public UnitStackElement(){val=new String(":unit:");}
		public String toString(){
		return val;
	}
	public UnitStackElement eval(Configuration c){return this;}
}
class IntegerStackElement extends StackElement<Integer>implements Evaluable{
	public IntegerStackElement(int v){
		val=new Integer(v);
	}
	public String toString(){
		return Integer.toString(val);
	}
	public IntegerStackElement eval(Configuration c){return this;}
}
class BooleanStackElement extends StackElement<Boolean>implements Evaluable{
	public BooleanStackElement(boolean v){
		val=new Boolean(v);
	}
	public String toString(){
		return (new String(":"+val.toString()+":"));
	}
	public BooleanStackElement eval(Configuration c){return this;}
}
class StringStackElement extends StackElement<String>implements Evaluable{
	public StringStackElement(String v){
		val=v;
	}
	public String toString(){
		String tmp=new String(val);
		return tmp.replace("\"", "");
	}
	public StringStackElement eval(Configuration c){return this;}
}
class NameStackElement extends StackElement<String>implements Evaluable{
	public NameStackElement(String str){
		val=str;
	}
	public String toString(){
		return val;
	}	
	public StackElement eval(Configuration c) throws Exception{
		if(!c.envs.empty()){
			Stack<HashMap<String, StackElement>> tmp=new Stack<HashMap<String, StackElement>>();
			while(!c.envs.empty()){
				HashMap<String, StackElement> e=c.envs.pop();
				tmp.push(e);
				if(e.containsKey(this.toString())){
					while(!tmp.isEmpty()){c.envs.push(tmp.pop());}
					return e.get(this.toString());
				}
			}
			while(!tmp.isEmpty()){c.envs.push(tmp.pop());}
			throw new AException(c);
		}
		else{
			throw new Exception("Empty stack of environments! This should not happen");
		}
	}
}
class ErrorStackElement extends StackElement<String>{
	public String toString(){
		return (new String(":"+"error"+":"));
	}
}
abstract class Operator<T0, T1>{
	abstract T1 op(T0 a, T0 b, Stack<StackElement> st) throws AException;
}
class Sum extends Operator<IntegerStackElement, IntegerStackElement> {
	public IntegerStackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st){return new IntegerStackElement(a.getValue()+b.getValue());}
}
class Sub extends Operator<IntegerStackElement,IntegerStackElement> {
	public IntegerStackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st){return new IntegerStackElement(b.getValue()-a.getValue());}
}
class Mul extends Operator<IntegerStackElement, IntegerStackElement> {
	public IntegerStackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st){return new IntegerStackElement(a.getValue()*b.getValue());}
}
class Div extends Operator<IntegerStackElement, StackElement>{
	public StackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st) throws AException{
	if (a.getValue()==0){
		throw new AException(null);//the value is not needed here
	}
	else {
		return new IntegerStackElement(b.getValue()/a.getValue());
		}
	}
}
class Rem extends Operator<IntegerStackElement, StackElement>{
	public StackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st)throws AException{
	if (a.getValue()==0){
			throw new AException(null);//the value is not needed here
	}
	else {
		return new IntegerStackElement(b.getValue()%a.getValue());
		}
	}
}
class Equal extends Operator<IntegerStackElement, BooleanStackElement>{
		public BooleanStackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st){return new BooleanStackElement(a.getValue().intValue()==b.getValue().intValue());}
}
class LessThan extends Operator<IntegerStackElement, BooleanStackElement>{
		public BooleanStackElement op(IntegerStackElement a, IntegerStackElement b, Stack<StackElement> st){return new BooleanStackElement(a.getValue().intValue()>b.getValue().intValue());}
}
class Cat extends Operator<StringStackElement, StringStackElement>{
		public StringStackElement op(StringStackElement a, StringStackElement b, Stack<StackElement> st){return new StringStackElement(b.toString()+a.toString());}
}
class And extends Operator<BooleanStackElement, BooleanStackElement>{
		public BooleanStackElement op(BooleanStackElement a, BooleanStackElement b, Stack<StackElement> st){return new BooleanStackElement(a.getValue().booleanValue()&&b.getValue().booleanValue());}
}
class Or extends Operator<BooleanStackElement, BooleanStackElement>{
		public BooleanStackElement op(BooleanStackElement a, BooleanStackElement b, Stack<StackElement> st){return new BooleanStackElement(a.getValue().booleanValue()||b.getValue().booleanValue());}
}
class AException extends Exception{
	Configuration c;
	public AException(Configuration c){this.c=c;}
}
interface Command{
	public Configuration exec(Configuration conf) throws Exception;
}
class Arithmetic implements Command{
	Operator<StackElement,StackElement> oper;
	public Arithmetic(Operator op){
		this.oper=op;
	}
	public Configuration exec(Configuration c) throws Exception{
		if (!c.stks.isEmpty()){
					if(c.stks.peek().size()>1){
						StackElement stel1=c.stks.peek().pop();
						StackElement stel2=c.stks.peek().pop();
						c.stks.peek().push(stel2);
						c.stks.peek().push(stel1);
						if (stel1 instanceof Evaluable) {
							Evaluable v1=(Evaluable)stel1;
							StackElement val1;
							try {val1=v1.eval(c);}
							catch(AException e){
								e.c.stks.peek().push(new ErrorStackElement());
								return c;								
							}
							if (stel2 instanceof Evaluable){
								Evaluable v2=(Evaluable)stel2;
								StackElement val2;
								try{val2=v2.eval(c);}
								catch(AException e){
									c.stks.peek().push(new ErrorStackElement());
									return c;								
								}
								if(val1 instanceof IntegerStackElement && val2 instanceof IntegerStackElement ||
								   val1 instanceof StringStackElement && val2 instanceof StringStackElement   ||
								   val1 instanceof BooleanStackElement && val2 instanceof BooleanStackElement){
									c.stks.peek().pop();
									c.stks.peek().pop();
									try{c.stks.peek().push(oper.op(val1,val2,
															   c.stks.peek()));}
									catch(AException e){
										c.stks.peek().push(stel2);
										c.stks.peek().push(stel1);
										c.stks.peek().push(new ErrorStackElement());
									}
									return c;
								}
								else{
									c.stks.peek().push(new ErrorStackElement());
									return c;
								}
							}
							else{
								c.stks.peek().push(new ErrorStackElement());
								return c;
							}
						}
						else{
							c.stks.peek().push(new ErrorStackElement());
							return c;
						}
					}
					else{
						c.stks.peek().push(new ErrorStackElement());
						return c;
					}
				}
		else{
			throw new Exception("Empty stack of stacks! It should not happen");
		}
	}
}
class Not implements Command{
	public Configuration exec(Configuration c) throws Exception{
		if (!c.stks.isEmpty()){
			if(c.stks.peek().size()>=1){
				StackElement st=c.stks.peek().pop();
				c.stks.peek().push(st);
				if(st instanceof Evaluable){
					Evaluable val=(Evaluable)st;
					try{
						StackElement stval=val.eval(c);
						if(stval instanceof BooleanStackElement){
							c.stks.peek().pop();
							BooleanStackElement stbool=(BooleanStackElement)stval;
							c.stks.peek().push(new BooleanStackElement(!stbool.getValue().booleanValue()));
						}
						else{
							c.stks.peek().push(new ErrorStackElement());
							return c;
						}
					}
					catch(AException e){
						e.c.stks.peek().push(new ErrorStackElement());
						return e.c;
					}
				}
				else{
					c.stks.peek().push(new ErrorStackElement());
					return c;					
				}
			}
			else{
				c.stks.peek().push(new ErrorStackElement());
				return c;
			}
		}
		else{
			throw new Exception("Empty stack of stacks! It should not happen");
		}
		return c;
	}
}

class Neg implements Command{
	public Configuration exec(Configuration c) throws Exception{
		if (!c.stks.isEmpty()){
			if(c.stks.peek().size()>=1){
				StackElement st=c.stks.peek().pop();
				c.stks.peek().push(st);
				if(st instanceof Evaluable){
					Evaluable val=(Evaluable)st;
					try{
						StackElement stval=val.eval(c);
						if(stval instanceof IntegerStackElement){
							c.stks.peek().pop();
							IntegerStackElement stint=(IntegerStackElement)stval;
							c.stks.peek().push(new IntegerStackElement((-1)*stint.getValue().intValue()));
						}
						else{
							c.stks.peek().push(new ErrorStackElement());
							return c;
						}
					}
					catch(AException e){
						e.c.stks.peek().push(new ErrorStackElement());
						return e.c;
					}
				}
				else{
					c.stks.peek().push(new ErrorStackElement());
					return c;					
				}
			}
			else{
				c.stks.peek().push(new ErrorStackElement());
				return c;
			}
		}
		else{
			throw new Exception("Empty stack of stacks! It should not happen");
		}
		return c;
	}
}

class Quit implements Command{
	private String oFile;
	public Quit(String str){this.oFile=str;}
	public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()){
			//We assume all the let are matched with their end so there should be only one external stack to write
			ArrayList<StackElement> alist = new ArrayList<StackElement>(conf.stks.pop());
			ListIterator<StackElement> itr_st=alist.listIterator(alist.size());
    		try{
    			File file = new File(oFile);
    			FileWriter fw = new FileWriter(file);
	  			BufferedWriter bw = new BufferedWriter(fw);
    			while(itr_st.hasPrevious()) {
    				itr_st.previous().writeToFile(bw);
    			}
    			bw.close();
    		}
    		catch(Exception e){
				e.printStackTrace();
    		}
			return conf;
		}
		else{
			throw new Exception("Empty stack of stacks! This should not happen");
		}
	}
}
class Pop implements Command{
	public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()){
			if(conf.stks.peek().size()>=1){
				conf.stks.peek().pop();
				return conf;
			}
			else{
				conf.stks.peek().push(new ErrorStackElement());
				return conf;
			}
		}
		else{
			throw new Exception("Empty stack of stacks! This should not happen");
		}
	}
}
class Push implements Command{
	private StackElement stel;
	public Push(StackElement stel){this.stel=stel;}
	public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()){
			conf.stks.peek().push(stel);
		}
		else{
			throw new Exception("Empty stack of stacks! This should not happen");
		}
		return conf;
	}
}
class Swap implements Command{
	public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()){
			if(conf.stks.peek().size()>=2){
				StackElement stel1 = conf.stks.peek().pop();
				StackElement stel2 = conf.stks.peek().pop();
				conf.stks.peek().push(stel1);
				conf.stks.peek().push(stel2);
				return conf;
			}
			else{
				conf.stks.peek().push(new ErrorStackElement());
				return conf;
			}
		}
		else{
			throw new Exception("Empty stack of stacks! This should not happen");
		}
	}
}

class Bind implements Command {
	public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()&&!conf.envs.isEmpty()){
			if(conf.stks.peek().size()>=2){
				StackElement stel1 = conf.stks.peek().pop();
				StackElement stel2 = conf.stks.peek().pop();
				if (stel2 instanceof NameStackElement && stel1 instanceof Evaluable){
					String name=stel2.toString();
						try{
							StackElement val=((Evaluable)stel1).eval(conf);
							conf.envs.peek().remove(name);			
							conf.envs.peek().put(name, val);
							conf.stks.peek().push(new UnitStackElement());		
						}					
						catch(AException c){
							conf.stks.peek().push(stel2);
							conf.stks.peek().push(stel1);
							conf.stks.peek().push(new ErrorStackElement());														
						}
						finally{return conf;}
				}
				else{
					conf.stks.peek().push(stel2);
					conf.stks.peek().push(stel1);
					conf.stks.peek().push(new ErrorStackElement());
				}
			}
			else{
				conf.stks.peek().push(new ErrorStackElement());
			}
		}
		else{
			throw new Exception("Empty stack of stacks or Stack of envs! This should not happen");
		}
		return conf;
	}
}
class Let implements Command{
	public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()&&!conf.envs.isEmpty()){
			conf.stks.push(new Stack<StackElement>());
			conf.envs.push(new HashMap<String, StackElement>());
		}
		else{
			throw new Exception("Empty stack of stacks or Stack of envs! This should not happen");
		}
		return conf;
	}
}
class End implements Command{
	public Configuration exec(Configuration conf)throws Exception{
		if(conf.stks.size()>1&&conf.envs.size()>1){
			StackElement stel;
			if (!conf.stks.peek().isEmpty()){stel=conf.stks.peek().pop();} else{stel=new ErrorStackElement();}
			conf.stks.pop();
			conf.envs.pop();
			conf.stks.peek().push(stel);
		}
		else{
			throw new Exception("Stack of stacks with size <=1. This should not happen");
		}
		return conf;
	}	
}
class If implements Command{
		public Configuration exec(Configuration conf) throws Exception{
		if(!conf.stks.isEmpty()){
			if(conf.stks.peek().size()>=3){
						StackElement stelt=conf.stks.peek().pop();
						StackElement stelf=conf.stks.peek().pop();
						StackElement stelg=conf.stks.peek().pop();
						conf.stks.peek().push(stelg);
						conf.stks.peek().push(stelf);
						conf.stks.peek().push(stelt);
						if(stelg instanceof Evaluable){
							Evaluable vg=(Evaluable) stelg;
							StackElement valg;
							try {valg =vg.eval(conf);}
							catch(AException e){
								conf.stks.peek().push(new ErrorStackElement());	
								return conf;
							}
							if(valg instanceof BooleanStackElement){
								BooleanStackElement bg=(BooleanStackElement) valg;
								conf.stks.peek().pop();
								conf.stks.peek().pop();
								conf.stks.peek().pop();
								if(bg.getValue().booleanValue()){
									conf.stks.peek().push(stelt);
									return conf;
								}
								else{
									conf.stks.peek().push(stelf);
									return conf;
								}
							}
							else{
								conf.stks.peek().push(new ErrorStackElement());
								return conf;								
							}
						}
						else{
							conf.stks.peek().push(new ErrorStackElement());
							return conf;
						}
			}
			else{
				conf.stks.peek().push(new ErrorStackElement());
				return conf;
			}
		}
		else{
			throw new Exception("Stack of stacks with size <=1. This should not happen");
		}
	}	
}
class Configuration{
	Stack<Stack<StackElement>> stks;
	Stack<HashMap<String,StackElement>> envs;
	ArrayList<Command> program;
	public Configuration(Stack<Stack<StackElement>> stks, Stack<HashMap<String,StackElement>> envs, ArrayList<Command> program){
		this.stks=stks;
		stks.push(new Stack<StackElement>());
		this.envs=envs;
		envs.push(new HashMap<String, StackElement>());
		this.program=program;
	}
}

class UtilityCommand{
		private String oFile, iFile;
		public UtilityCommand(String in, String out){this.iFile=in;this.oFile=out;}
		public StackElement stringToStackElement(String str){
			if (str.matches("\".+\"")){
					return new StringStackElement(str);
			}
			else if (str.matches("(-)?\\d+")) {
				return new IntegerStackElement(Integer.parseInt(str));
			}
			else if (str.matches("(_)?[a-z|A-Z][a-z|A-Z|0-9|_]*")){
				return new NameStackElement(str);	
			}
			else if (str.matches(":true:") ){
				return new BooleanStackElement(true);
			}
			else if (str.matches(":false:") ){
				return new BooleanStackElement(false);
			}
			else if (str.matches(":error:") ){
				return new ErrorStackElement();
			}
			return new ErrorStackElement(); 
		}
		public Command stringToCommand(String str)throws Exception{
			String spl[] = str.split(" ", 2);
			switch(spl[0]){
				case "end":
					return new End();
				case "if":
					return new If();
				case "add":
					return new Arithmetic(new Sum());
				case "sub":
					return new Arithmetic(new Sub());
				case "mul":
					return new Arithmetic(new Mul());
				case "div":
					return new Arithmetic(new Div());
				case "rem":
					return new Arithmetic(new Rem());
				case "neg":
					return new Neg();
				case "not":
					return new Not();
				case "swap":
					return new Swap();
				case "pop":
					return new Pop();
				case "push":
					 return new Push(stringToStackElement(spl[1]));
				case "quit":
					return new Quit(this.oFile);
				case "and":
					return new Arithmetic(new And());
				case "or":
					return new Arithmetic(new Or());
				case "cat":
					return new Arithmetic(new Cat());
				case "let":
					return new Let();
				case "bind":
					return new Bind();
				case "equal":
					return new Arithmetic(new Equal());
				case "lessThan":
					return new Arithmetic(new LessThan());					
				default: 
					throw new Exception("Can't parse command");
			}
		}
	public ArrayList<Command> getProgram(){
			ArrayList<Command> program=new ArrayList<Command>();
			try (BufferedReader br = new BufferedReader(new FileReader(this.iFile))) {
    			String line;
    			while ((line = br.readLine()) != null) {
       				program.add(this.stringToCommand(line));
    			}
    			br.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return program;
		}
}
public class interpreter{
	public static void interpreter(String iFile, String oFile) throws Exception{
			UtilityCommand ut=new UtilityCommand(iFile, oFile);
			Configuration conf=new Configuration(new Stack<Stack<StackElement>>(), new Stack<HashMap<String, StackElement>>(),ut.getProgram());
			Iterator<Command> itr_ex=conf.program.iterator();
			while(itr_ex.hasNext()) {
      				conf=itr_ex.next().exec(conf);
    		}
	}
//	public static void main(String[] args) throws Exception{
//		interpreter(args[0], args[1]);
//	}
}