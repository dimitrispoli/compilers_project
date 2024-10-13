import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.io.FileWriter;


public class Main {

    public static LinkedHashMap<String, LinkedHashMap<String, String>> smap;
    
    public static LinkedHashMap<String,Integer> var_offset=new LinkedHashMap<String,Integer>();

    public static LinkedHashMap<String,Integer> method_offset=new LinkedHashMap<String,Integer>();

    //public static LinkedHashMap<String ,Integer> offsets=new LinkedHashMap<String, Integer>();

    public static LinkedHashMap<String, LinkedHashMap<String, String>> vtables=new LinkedHashMap<String, LinkedHashMap<String, String>>();

    public static LinkedHashMap<String, LinkedHashMap<String, Integer>> vartables=new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
    
    public static LinkedHashMap<String,String> keep_new;

    public static String special_line;

    public static String line_to_write;

    public static String file_name;

    public static int reg;
    public static int nsz;
    public static int oob;
    public static int if_then;
    public static int exp_res;
    public static int whil;

    public static void init_reg(){
        reg=0;
        nsz=0;
        oob=0;
        if_then=0;
        exp_res=0;
        whil=0;
    }

    public static void main(String[] args) throws Exception {
        



        FileInputStream fis = null;
        try{
            for (String s: args){
            
            fis = new FileInputStream(s);
            MiniJavaParser parser = new MiniJavaParser(fis);

            Goal root = parser.Goal();

            //System.err.println("Program parsed successfully.");
            
            
            MyVisitor eval = new MyVisitor();
            try{
                smap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
                vartables=new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
                vtables=new LinkedHashMap<String, LinkedHashMap<String, String>>();
                keep_new= new LinkedHashMap<String, String>();
                root.accept(eval, null);
                int dot_pos=s.lastIndexOf(".");
                String new_name=s.substring(0,dot_pos);
                String actual_new_name=String.format("%s.ll",new_name);
                file_name=actual_new_name;
                init_reg();
                //myfile = new FileWriter(actual_new_name);
                line_to_write="declare i8* @calloc(i32, i32)\n"
                +"declare i32 @printf(i8*, ...)\n"
                +"declare void @exit(i32)\n\n"
                +"@_cint = constant [4 x i8] c\"%d\\"+"0a\\"+"00\" \n"
                +"@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"
                +"@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"\n"
                +"define void @print_int(i32 %i) {\n"
                +"\t%_str = bitcast [4 x i8]* @_cint to i8*\n"
                +"\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"
                +"\tret void\n"
                +"}\n\n"
                +"define void @throw_oob() {\n "
                +"\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n "
                +"\tcall i32 (i8*, ...) @printf(i8* %_str)\n "
                +"\tcall void @exit(i32 1)\n\tret void\n}\n"
                +"define void @throw_nsz() {\n\t%_str = bitcast [15 x i8]* @_cNSZ to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n\n";

                line_to_write=line_to_write+"@."+new_name+"_vtable = global [0 x i8*] []\n\n";
                //myfile.write(line);
                //myfile.close();
                //System.out.println(smap);
                //MyOtherVisitor check = new MyOtherVisitor();
                //root.accept(check,null);
            
                int i=0;
            for (Map.Entry<String,LinkedHashMap<String, String>> entry1 : smap.entrySet()){
                String key1=entry1.getKey();
                String[] splited1=key1.split("\\s+");
                //System.out.println(splited1[1]+" "+ key1+ " : "+var_offset);
                var_offset.put(splited1[1],0);
                method_offset.put(splited1[1],0);
                if(splited1[0].equals("Class")&&i!=0){
                    LinkedHashMap<String, String> class_map=entry1.getValue();
                    if(class_map.containsKey("extends")){
                        String starting_class=class_map.get("extends");
                        String[] starting_class_spl=starting_class.split("\\s+");
                        var_offset.put(splited1[1],var_offset.get(starting_class_spl[1]));
                        method_offset.put(splited1[1],method_offset.get(starting_class_spl[1]));
                        //System.out.println(splited1[1]+"."+ var_offset.get(starting_class_spl[1])+ " : "+method_offset.get(starting_class_spl[1]));
                    }
                    
                    
                    if(class_map.containsKey("extends")){
                        String extending_class=class_map.get("extends");
                        String[] extending_class_spl=extending_class.split("\\s+");
                        LinkedHashMap<String, Integer> v1;
                        v1=vartables.get(extending_class_spl[1]);
                        LinkedHashMap<String, Integer> v1_temp=new LinkedHashMap<String, Integer>(v1);
                        vartables.put(splited1[1],v1_temp);

                        LinkedHashMap<String, String> v2;
                        v2=vtables.get(extending_class_spl[1]);
                        LinkedHashMap<String, String> v2_temp=new LinkedHashMap<String, String>(v2);
                        vtables.put(splited1[1],v2_temp);
                    }
                    else{
                        LinkedHashMap<String, Integer> v1=new LinkedHashMap<String, Integer>();
                        v1.put("Sum ",0);
                        vartables.put(splited1[1],v1);

                        LinkedHashMap<String, String> v2=new LinkedHashMap<String, String>();
                        vtables.put(splited1[1],v2);
                    }
                    for (Map.Entry<String,String> entry : class_map.entrySet()){
                        String key=entry.getKey();
                        String[] splited=key.split("\\s+");
                        String val=entry.getValue();
                        
                        

                        if(splited.length==1){
                            LinkedHashMap<String, Integer> v1=vartables.get(splited1[1]);
                            if(val.equals("int")){
                                int temp=var_offset.get(splited1[1]);
                                //System.out.println(splited1[1]+"."+ key+ " : "+temp);
                                v1.put(key,temp+8);
                                v1.put("Sum ",4+v1.get("Sum "));
                                var_offset.put(splited1[1],temp+4);
                            }
                            else if(val.equals("boolean")){
                                //System.out.println(splited1[1]+"."+ key+ " : "+var_offset.get(splited1[1]));
                                v1.put(key,var_offset.get(splited1[1])+8);
                                v1.put("Sum ",1+v1.get("Sum "));
                                var_offset.put(splited1[1],var_offset.get(splited1[1])+1);
                            }
                            else if(!key.equals("extends")){
                                //System.out.println(splited1[1]+"."+ key+ " : "+var_offset.get(splited1[1]));
                                v1.put(key,var_offset.get(splited1[1])+8);
                                v1.put("Sum ",8+v1.get("Sum "));
                                var_offset.put(splited1[1],var_offset.get(splited1[1])+8);
                            }

            
                        
                        }
                        else{
                            LinkedHashMap<String, String> v2=vtables.get(splited1[1]);
                            if(class_map.containsKey("extends")){
                                String extend=class_map.get("extends");
                                String temp=Main.getscope(splited[0]+" "+extend+" "+splited[3],extend);
                                if(temp==null){
                                    //System.out.println(splited[2]+"."+ splited[3]+ " : "+method_offset.get(splited[2]));
                                    //v2.put("Class ",splited[2]);
                                    method_offset.put(splited[2],method_offset.get(splited[2])+8);
                                    v2.put(splited[3],Integer.toString(method_offset.get(splited[2])));
                                    //method_offset.put(splited[2],(method_offset.get(splited[2])-8)/8);
                                    v2.put(splited[3],splited[2]+" "+Integer.toString((method_offset.get(splited[2])-8)/8));
                                    //System.out.println("end of here if"+v2);
                                }
                                else{
                                    //System.out.println("Here now "+temp);
                                    String[] split_temp=temp.split("\\s+");
                                    //System.out.println("Here now "+split_temp[1]);
                                    LinkedHashMap<String, String> extend_vtable=vtables.get(split_temp[1]);
                                    String method_offs=extend_vtable.get(splited[3]);
                                    String[] method_offs_spl=method_offs.split("\\s+");
                                    v2.put(splited[3],splited[2]+" "+method_offs_spl[1]);
                                    //System.out.println("end of here "+v2);
                                }
                                

                            }
                            else{
                                //System.out.println(splited[2]+"."+ splited[3]+ " : "+method_offset.get(splited[2]));
                                method_offset.put(splited[2],method_offset.get(splited[2])+8);
                                v2.put(splited[3],splited[2]+" "+Integer.toString((method_offset.get(splited[2])-8)/8));
                                //System.out.println("end of here else "+v2);
                            }
                        }
                    }
                }
                i++;
                
            }
            //System.out.println(smap);
            //System.out.println(vartables);
            //System.out.println(vtables);
            for (Map.Entry<String,LinkedHashMap<String, String>> entry1 : vtables.entrySet()){
                String key1=entry1.getKey();
                LinkedHashMap<String, String> val1=entry1.getValue();
                line_to_write=line_to_write+"@."+key1+"_vtable = global ["+Integer.toString(val1.size())+" x i8*] [\n";
                int count=1;
                for(Map.Entry<String,String> entry :val1.entrySet()){
                    String key=entry.getKey();
                    String val=entry.getValue();
                    String[] split_val=val.split("\\s+");
                    String actual_class=split_val[0];
                    //System.out.println("At least here "+actual_class);
                    LinkedHashMap<String, String> method_class=smap.get("Method Class "+actual_class+" "+key);
                    //System.out.println("At least here: "+"Method Class "+actual_class+" "+key);
                    String type=method_class.get("Type ");
                    String arguments_=method_class.get("Arg ");
                    String[] arguments=arguments_.split("\\s+");

                    line_to_write=line_to_write+"\ti8* bitcast ("+Main.gettype(type)+" (i8*";
                    //System.out.println(arguments_);
                    if(!arguments_.isEmpty()){
                        for(String a:arguments){
                            if(!a.equals("Class")){
                            //System.out.println("\n"+a+" " + Main.gettype(a)+"\n");
                            line_to_write=line_to_write+","+Main.gettype(a);
                            //System.out.println(line_to_write);
                            }
                        }
                    }
                    line_to_write=line_to_write+")* @"+actual_class+"."+key+" to i8*)";
                    if(val1.size()!=count){
                        line_to_write=line_to_write+",";
                    }
                    count++;
                    line_to_write=line_to_write+"\n";
                }
                line_to_write=line_to_write+"]\n\n";
                //System.out.println(line_to_write);
                
            }

            MyOtherVisitor check = new MyOtherVisitor();
            root.accept(check,null);
            //System.out.println("After root");
            
            //System.out.println(smap);
            //System.out.println(vartables);
            //System.out.println(vtables);
            FileWriter myfile = new FileWriter(file_name);
            myfile.write(line_to_write);
            myfile.close();

            }
            catch(Exception ex){
                System.out.println("File: " +s +" Something went wrong\n");
                //System.out.println(Main.line_to_write);
            }

            fis.close();
        
            }
              
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        catch(Exception ex){
            System.out.println("filefailed");
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }  
        
        
    }

    public static String gettype(String type){
        if(type.equals("int")){
            return "i32";
        }
        if(type.equals("boolean")){
            return "i1";
        }
        if(type.equals("boolean*")||type.equals("boolean[]")){
            return "i32*";
        }
        if(type.equals("int*")||type.equals("int[]")){
            return "i32*";
        }
        if(type.equals("char")){
            return "i8";
        }
        if(type.equals("char*")){
            return "i8*";
        }
        return "i8*";
    }
    
    public static boolean checkoverload(String m, String type1, String arg1){
        


        //System.out.println("Overload:String: "+m +" "+type1+" "+arg1);
        //check if it exist with another name
        String[] splited= m.split("\\s+");
        String new_class=splited[1]+" "+splited[2];
        LinkedHashMap<String, String> new_scope=smap.get(new_class);    //get current class it belongs to
        if(new_scope==null){                                            //if it doesn't exist yet the order is wrong anyway
            return true;
        }
        //System.out.println("Overload: Curr_class: "+new_class +" "+type1+" "+arg1);
        if(new_scope.containsKey("extends")){                           //if it is an extension then check for more declarations
            new_class=new_scope.get("extends");
            LinkedHashMap<String, String> m_map=smap.get("Method "+new_class+" "+splited[3]);
            //System.out.println("Overload:String: "+"Method "+new_class+" "+splited[3] +" "+type1+" "+arg1);
            if(null==m_map){                                            //if it doesn't exist in the upper class maybe it exist later
                return Main.checkoverload("Method "+new_class+" "+splited[3],type1,arg1);
            }
            else{                                                       //if a diferent declaration exists make sure it is correct
                //System.out.println("Overload:String: "+"Method "+new_class+splited[3] +" "+type1+" "+arg1);
                String arg2=m_map.get("Arg ");
                String type2=m_map.get("Type ");
                if(arg2.equals(arg1)&& type2.equals(type1)){
                    return false;
                }
                return true;
            }
            
        }
        else{                                   //no need to search
            return false;
        }
        
        
    }
    
    public static LinkedHashMap<String, String> getmap(String n){
        return smap.get(n);
    }    

    public static String load(String cl1,String type,String[] scopes){
        //System.out.println("IN load String= "+cl1);
        if(cl1.charAt(0)!='%'){
            return cl1;
        }
        String[] scopes_split=scopes[0].split("\\s++");
        LinkedHashMap<String, Integer> vartable=vartables.get(scopes_split[1]);
        //System.out.println("IN load String= "+vartable);
        if(vartable==null){         //if not a class variable
            if(cl1.charAt(1)=='_'||cl1.equals("%this")){
                return cl1;
            }
            //System.out.println("IN load type= "+type);
            int reg_temp=reg++;
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load %s, %s* %s\n",reg_temp,type,type,cl1);
            cl1=String.format("%%_%d",reg_temp);
            return cl1;
        }
        if(!vartable.containsKey(cl1.substring(1))){
            if(cl1.charAt(1)=='_'||cl1.equals("%this")){
                return cl1;
            }
            //System.out.println("IN load type= "+type);
            int reg_temp=reg++;
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load %s, %s* %s\n",reg_temp,type,type,cl1);
            cl1=String.format("%%_%d",reg_temp);
        }
        
        return cl1;
    }

    public static String getdata(String cl1,String type,String[] scopes,String use){
        //System.out.println("IN load String= "+cl1+" Scope "+scopes[0]);
        if(cl1.charAt(0)!='%'||cl1.equals("%this")){
            return cl1;
        }
        String[] scopes_split=scopes[0].split("\\s++");
        LinkedHashMap<String, Integer> vartable=vartables.get(scopes_split[1]);
        //System.out.println("IN load String= "+vartable);
        if(vartable==null){
            return cl1;
        }
        if(vartable.containsKey(cl1.substring(1))){
            int reg_temp=Main.reg++;
            int reg_bit=Main.reg++;
            //System.out.println("IN load String= "+type);
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d= getelementptr i8, i8* %%this, i32 %d\n",reg_temp,vartable.get(cl1.substring(1)));//vartable.get(cl1.substring(1))
            //System.out.println("IN load String= "+vartable);
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d =  bitcast i8* %%_%d to %s*\n",reg_bit,reg_temp,type);
            if(use.equals("use")){
                int reg_load=Main.reg++;
                Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load %s, %s* %%_%d\n",reg_load,type,type,reg_bit);
                reg_bit=reg_load;
            }
            //ln("IN load String= before send off of"+cl1+" "+String.format("%%_%d",reg_bit));
            cl1=String.format("%%_%d",reg_bit);
        }
        
        return cl1;
    }

    

    public static Boolean issubclass(String clas,String subclass){
        //System.out.println("death "+clas+" "+subclass+" ");
        if(clas.equals(subclass)){
            return true;
        }
        LinkedHashMap<String, String> subclass_scope=smap.get(subclass);
        //System.out.println("death "+clas+" "+subclass+" "+subclass_scope);
        if(subclass_scope==null){
            return false;
        }
        while(subclass_scope.containsKey("extends")){
            subclass= subclass_scope.get("extends");
            if(clas.equals(subclass)){
                return true;
            }
            subclass_scope=smap.get(subclass);
        }
        return false;
    }

    public static String checkinscope(String n, String scope){
        if(scope.equals(n)){
            return n;
        }
        LinkedHashMap<String, String> s=smap.get(scope);
        //System.out.println("n and scope-- "+ n+" "+scope);
        if(s.containsKey(n)){
            String[] n_spl = n.split("\\s+");
            
            return s.get(n);
        }
        ArrayList<String> v= new ArrayList<String>();
        v.add(scope);
        while(s.containsKey("extends")){
            scope=s.get("extends");
            if(v.contains(scope)){
                return null;
            }
            v.add(scope);
            if(scope.equals(n)){
                return n;
            }
            s=smap.get(scope);
            String[] splited =n.split("\\s+");
            if(splited.length>1){
                n=splited[0]+" "+scope+" "+splited[3];
            } 
            //System.out.println("n and scope-- "+ n+" "+scope);
            if(s.containsKey(n)){
                return s.get(n);
            }
        }
        return null;
    }

    

    public static String getscope(String n, String scope){// only defference with check scope is it returns the class(scope) it was found in

        if(scope.equals(n)){
            return n;
        }
        LinkedHashMap<String, String> s=smap.get(scope);
        //System.out.println("n and scope-- get scope this time "+ n+" "+scope);
        if(s.containsKey(n)){
            return scope;
        }
        ArrayList<String> v= new ArrayList<String>();
        v.add(scope);
        while(s.containsKey("extends")){
            scope=s.get("extends");
            if(v.contains(scope)){
                return null;
            }
            v.add(scope);
            if(scope.equals(n)){
                return scope;
            }
            s=smap.get(scope);
            String[] splited =n.split("\\s+");
            if(splited.length>1){
                n=splited[0]+" "+scope+" "+splited[3];
            } 
            //System.out.println("n and scope-- "+ n+" "+scope);
            if(s.containsKey(n)){
                return scope;
            }
        }
        return null;
    }

    public static Boolean circularextend( String scope){

        LinkedHashMap<String, String> s=smap.get(scope);
        //System.out.println("Circular: " + scope+" "+ s);
        ArrayList<String> v= new ArrayList<String>();
        v.add(scope);
        while(s.containsKey("extends")){
            scope=s.get("extends");
            //System.out.println("Circular: " + scope+" "+ s);
            if(v.contains(scope)){
                return true;
            }
            v.add(scope);
            s=smap.get(scope);
            //System.out.println("Circular: " + scope+" "+ s);
            if(s==null){
                return false; //not ready to tell yet
            }
        }
        return false;
    }

    public static String existsinscopes(String n, String[] scopes){
        if (scopes[1]!=null){
            //System.out.println("PExists before check -- "+ n);
            String temp=checkinscope(n, scopes[1]);
            //System.out.println("PExists after check -- "+ temp);
            if(!(null==temp)){
                return temp;
            }
        }
        if (scopes[0]!=null){
            String temp=checkinscope(n, scopes[0]);
            if(!(null==temp)){
                return temp;
            }
        }
        return null;
    }
}


class MyVisitor extends GJDepthFirst<String, String>{
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String s) throws Exception {
        String classname = n.f1.accept(this, null);
        //System.out.println("Class: " + classname);
        
        LinkedHashMap<String,String> classname_map = new LinkedHashMap<String,String>();
        Main.smap.put("Class "+classname,classname_map);


        if(n.f14.present()){
            n.f14.accept(this,"Class "+classname);
        }
        

        //System.out.println();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String c) throws Exception {
        String classname = n.f1.accept(this, null);
        //System.out.println("Class: " + classname);

        LinkedHashMap<String,String> classname_map = new LinkedHashMap<String,String>();
        if(Main.smap.containsKey("Class "+classname)){
            throw new  ParseException();
        }
        else{
            Main.smap.put("Class "+classname,classname_map);
        }

        if(n.f3.present()){
            //System.out.println("lover"  + classname);
            n.f3.accept(this, "Class " + classname);
        }
        if(n.f4.present()){
            //System.out.println("lover"  + classname);
            n.f4.accept(this,"Class " + classname);
        }
        
        //System.out.println();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String c) throws Exception {
        //System.out.println("ClassExtends: ");
        LinkedHashMap<String, String> argu=Main.getmap(c);
        String classname = n.f1.accept(this, null);
        String extend = n.f3.accept(this, null);
        
        
        //System.out.println("Class " + classname+" "+ extend);

        LinkedHashMap<String,String> classname_map = new LinkedHashMap<String,String>();
        Main.smap.put("Class "+classname,classname_map);
        classname_map.put("extends","Class "+extend);

        if(n.f5.present()){
            n.f5.accept(this, "Class " + classname);
        }
        if(n.f6.present()){
            n.f6.accept(this, "Class " + classname);
        }

        
        
        

        if(Main.circularextend("Class "+classname)){
            throw new ParseException();
        }



        //System.out.println();

        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n,  String c) throws Exception {
        LinkedHashMap<String, String> argu=Main.getmap(c);
        String myType=n.f0.accept(this,null);
        String myName=n.f1.accept(this,null);
        //System.out.println("Var declaration of  " + myType+" "+ myName+" "+ argu);
        if(argu.containsKey(myName)){
            throw new ParseException();
        }
        else{

            if(myType.equals("int")||myType.equals("boolean[]")||myType.equals("boolean")||myType.equals("int[]")){
                argu.put(myName,myType);
            }
            else{
                argu.put(myName,"Class "+myType);
            }
            //System.out.println("Var declaration of  " + myType+" "+ myName+" "+ argu);
        }
        
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n,  String c) throws Exception {
        LinkedHashMap<String, String> argu=Main.getmap(c);
        LinkedHashMap<String,String> method_map = new LinkedHashMap<String,String>();
        
        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        
        Main.smap.put("Method "+c+" "+myName,method_map);

        
        String argumentList = n.f4.present() ? n.f4.accept(this, "Method "+c+" "+myName) : "";
        
        if(n.f7.present()){
            //System.out.println("Class: Wahahhahahfafebwhfbaejh ");
            n.f7.accept(this, "Method "+c+" "+myName);
        }
        if(argu.containsKey("Method "+c+" "+myName)){
            throw new  ParseException();
        }
        else{
            method_map.put("Arg ", argumentList);
            if(myType.equals("int")||myType.equals("boolean[]")||myType.equals("boolean")||myType.equals("int[]")){
                argu.put("Method "+c+" "+myName,myType);
                method_map.put("Type ", myType);
                if(Main.checkoverload("Method "+c+" "+myName,myType,argumentList)){
                    throw new ParseException();
                }
            }
            else{
                argu.put("Method "+c+" "+myName,"Class "+myType);
                method_map.put("Type ", "Class "+myType);
                if(Main.checkoverload("Method "+c+" "+myName,"Class "+myType,argumentList)){
                    throw new ParseException();
                }
            }
        }
        
        

        
        
        
        //System.out.println(myType + " " + myName + " -- " + argumentList+" "+Main.smap);
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n,  String c) throws Exception {
        String ret = n.f0.accept(this, c);

        if (n.f1 != null) {
            ret += n.f1.accept(this, c);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, String c) throws Exception {
        return n.f1.accept(this, c);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String c) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += " " + node.accept(this, c);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String c) throws Exception{
        String type = n.f0.accept(this, c);
        String name = n.f1.accept(this, c);
        LinkedHashMap<String, String> argu=Main.getmap(c);
        if(argu.containsKey(name)){
            throw new ParseException();
        }
        else{
            if(type.equals("int")||type.equals("boolean[]")||type.equals("boolean")||type.equals("int[]")){
                argu.put(name,type);
                return type;
            }
            else{
                argu.put(name,"Class "+type);
                return "Class "+type;
            }
        }
        
    }

    @Override
    public String visit(BooleanArrayType n,  String c) {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, String c) {
        return "int[]";
    }

    public String visit(BooleanType n,  String c) {
        return "boolean";
    }

    public String visit(IntegerType n, String c) {
        return "int";
    }

    @Override
    public String visit(Identifier n,  String c) {
        return n.f0.toString();
    }
}


//MY OTHER VISITOR


class MyOtherVisitor extends GJDepthFirst<String,  String[]>{

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String[] scope) throws Exception {
        //System.out.println("MainDecl   -- ");
        Main.init_reg();
        scope= new String[2];
        String myName = n.f1.accept(this, null);
        myName=myName.substring(1);
        scope[0]="Class "+ myName;  //current class
        scope[1]=null;              //current method
        Main.line_to_write=Main.line_to_write+"define i32 @main() {\n";
        n.f14.accept(this,scope);
        if(n.f15.present()){
            n.f15.accept(this, scope);
        }
        Main.line_to_write=Main.line_to_write+"\tret i32 0\n}\n\n";
        return null;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String[] scope) throws Exception {
        //System.out.println("ClassDecl   -- ");
        Main.init_reg();
        scope= new String[2];
        String myName = n.f1.accept(this, null);
        myName=myName.substring(1);
        scope[0]="Class "+ myName;  //current class
        scope[1]=null;              //current method
        //n.f3.accept(this,scope);
        n.f4.accept(this,scope);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    @Override
    public String visit(ClassExtendsDeclaration n, String[] scope) throws Exception {
        //System.out.println("ClassExtendsDecl   -- ");
        Main.init_reg();
        scope= new String[2];
        String myName = n.f1.accept(this, null);
        myName=myName.substring(1);
        scope[0]="Class "+ myName;  //current class
        scope[1]=null;              //current method
        //n.f5.accept(this,scope);
        if(n.f6.present()){
            n.f6.accept(this, scope);
        }
        return null;
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    @Override
    public String visit(MethodDeclaration n, String[] scope) throws Exception {
        //System.out.println("Methoddecl   -- ");
        Main.init_reg();
        String myType = n.f1.accept(this, scope);
        String myName = n.f2.accept(this, scope);
        myName=myName.substring(1);
        
        String par;
        if(n.f4.present()){
            par = n.f4.accept(this, scope);
        }
        else{
            par=null;
        }
        
        //System.out.println("After accept   -- ");
        scope[1]="Method "+ scope[0]+" "+myName;
        //System.out.println(" " + myName + " -- " + scope[0]);
        String[] scope_split=scope[0].split("\\s++");
        Main.line_to_write=Main.line_to_write+"define "+Main.gettype(myType)+" @"+scope_split[1] +"."+myName+"(i8* %this";
        LinkedHashMap<String,String> method_class=Main.smap.get(scope[1]);
        String type=method_class.get("Type ");
        String arguments_=method_class.get("Arg ");
        String[] arguments=arguments_.split("\\s+");

        //System.out.println(arguments_);
        String store_pars="\n";
        if(!arguments_.isEmpty()){
            int count=1;
            String[] split_par=par.split("\\s+");
            //System.out.println(par);
            for(String a:arguments){
                //System.out.println(count);
                if(!a.equals("Class")){
                    //System.out.println("\n"+a+" " + Main.gettype(a)+"\n");

                    Main.line_to_write=Main.line_to_write+", "+Main.gettype(a)+" %."+split_par[count].substring(1);
                    store_pars=store_pars+"\t"+split_par[count]+" = alloca "+Main.gettype(a)+"\n\tstore "+Main.gettype(a)+" %."+split_par[count].substring(1)+", "+Main.gettype(a)+"* "+split_par[count]+"\n";
                    
                    
                    //System.out.println(line_to_write);
                    count=count+2;
                }
                
            }
        }
        Main.line_to_write=Main.line_to_write+") {\n";
        Main.line_to_write=Main.line_to_write+store_pars;
        n.f7.accept(this,scope);
        n.f8.accept(this,scope);
        
        String ex_type=n.f10.accept(this, scope);
        
        
        ex_type=Main.load(ex_type,Main.gettype(myType),scope);
        ex_type=Main.getdata(ex_type,Main.gettype(myType),scope,"use");
        Main.line_to_write=Main.line_to_write+"\tret "+Main.gettype(myType)+" "+ex_type+"\n}\n\n";
        //String[] spl_ex= ex_type.split("\\s+");
        //System.out.println(" " + myType + " -- " + scope[0]);
        /*if(spl_ex[0].equals("Class")){
            if(!Main.issubclass("Class "+myType,ex_type)){
                throw new ParseException();
            }
        }
        else{
            if(!(spl_ex[0].equals(myType))){
                throw new ParseException();
            }
        }*/
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n,  String[] scope) throws Exception {
        //LinkedHashMap<String, String> argu=Main.getmap(c);
        String myType=n.f0.accept(this,null);
        String myName=n.f1.accept(this,null);
        Main.line_to_write=Main.line_to_write+String.format("\t%s = alloca %s\n",myName,Main.gettype(myType));
        return null;
    }
    /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    @Override
    public String visit(Statement n, String[] scope) throws Exception {
        return n.f0.accept(this, scope);
    }

    /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    @Override
    public String visit(Block n, String[] scope) throws Exception {
        return n.f1.accept(this, scope);
    }

    

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    @Override
    public String visit(AssignmentStatement n, String[] scope) throws Exception {
        //System.out.println("AssignStatement   -- ");
        String var_=n.f0.accept(this, scope);
        //System.out.println("Assignexpression   -- again");
        String exp=n.f2.accept(this, scope);
        //System.out.println("Assingexpression   -- "+ var_);
        String var=var_.substring(1);
        //System.out.println("Assingexpression   -- "+ var);
        String var_type= Main.existsinscopes(var,scope);
        
        //System.out.println("Assingexpression   -- here "+ var_type+" "+Main.gettype(var_type));
        exp=Main.load(exp,Main.gettype(var_type),scope);
        exp=Main.getdata(exp,Main.gettype(var_type),scope,"use");
        var=Main.load(var_,Main.gettype(var_type),scope);
        var=Main.getdata(var_,Main.gettype(var_type),scope,"no");
        
        if(var_type.equals("boolean")&&Main.gettype(var_type).equals("i38*")){
            //System.out.println("Assignexpression   -- is boolean");
            int reg_bool_case=Main.reg++;
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = trunc %s* %s to i1\n",reg_bool_case,Main.gettype(var_type),var,var);
            exp=String.format("%%_%d",reg_bool_case);
        }
        Main.line_to_write=Main.line_to_write+String.format("\tstore %s %s, %s* %s\n",Main.gettype(var_type),exp,Main.gettype(var_type),var);
        //System.out.println(String.format("\tstore %s %s, %s* %s\n",Main.gettype(var_type),exp,Main.gettype(var_type),var));
        //System.out.println("AssignStatement   -- end");
        
        
        return exp;
    }

     /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    @Override
    public String visit(ArrayAssignmentStatement n, String[] scope) throws Exception {
        //System.out.println("ArrayAssignStatement   -- ");
        String e1=n.f0.accept(this, scope);
        String var_type= Main.existsinscopes(e1.substring(1),scope);
        
        
        
        String e2=n.f2.accept(this, scope);
        String e3=n.f5.accept(this, scope);
        
        e1=Main.load(e1,"i32*",scope);
        e1=Main.getdata(e1,"i32*",scope,"use");
       
        


        //int reg_add=Main.reg++;
        int reg_size=Main.reg++;
        int reg_cmp_nz=Main.reg++;
        int reg_cmp_oob=Main.reg++;
        int reg_cmp=Main.reg++;
        int label_oob_ok=Main.oob++;
        int label_oob_error=label_oob_ok;
        int reg_offset=Main.reg++;
        int reg_element=Main.reg++;
        
        
        //System.out.println("ArrayAssingexpression   -- here "+ var_type);
        
        //Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32*, i32** %s\n",reg_add,e1);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32, i32* %s\n",reg_size,e1);

        e2=Main.load(e2,"i32",scope);
        e2=Main.getdata(e2,"i32",scope,"use");

        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp sge i32 %s, 0\n",reg_cmp_nz,e2);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp slt i32 %s, %%_%d\n", reg_cmp_oob,e2,reg_size);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = and i1 %%_%d, %%_%d\n",reg_cmp,reg_cmp_nz,reg_cmp_oob);
        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %%_%d, label %%oob_ok_%d, label %%oob_err_%d\n",reg_cmp,label_oob_ok,label_oob_error);
        Main.line_to_write=Main.line_to_write+String.format("\toob_err_%d:\n\tcall void @throw_oob()\n\tbr label %%oob_ok_%d\n\toob_ok_%d:\n",label_oob_ok,label_oob_ok,label_oob_ok);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = add i32 1, %s\n",reg_offset,e2);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = getelementptr i32, i32* %s, i32 %%_%d\n",reg_element,e1,reg_offset);

        e3=Main.load(e3,"i32",scope);
        e3=Main.getdata(e3,"i32",scope,"use");

        Main.line_to_write=Main.line_to_write+String.format("\tstore i32 %s, i32* %%_%d\n\n",e3,reg_element);
        
        return null;
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    @Override
    public String visit(IfStatement n, String[] scope) throws Exception {
        //System.out.println("IfStatement   -- ");
        int reg_if=Main.if_then++;
        String e1=n.f2.accept(this, scope);
        e1=Main.load(e1,"i1",scope);
        e1=Main.getdata(e1,"i1",scope,"use");
        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %s, label %%if_then_%d, label %%if_else_%d\n",e1,reg_if,reg_if);
        Main.line_to_write=Main.line_to_write+String.format("\tif_else_%d:\n",reg_if);
        n.f6.accept(this, scope);
        //Main.line_to_write=Main.line_to_write+String.format("\tif_then_%d:\n",reg_if);
        Main.line_to_write=Main.line_to_write+String.format("\tbr label %%if_end_%d\n",reg_if);
        Main.line_to_write=Main.line_to_write+String.format("\tif_then_%d:\n",reg_if);
        n.f4.accept(this, scope);
        Main.line_to_write=Main.line_to_write+String.format("\tbr label %%if_end_%d\n",reg_if);
        Main.line_to_write=Main.line_to_write+String.format("\tif_end_%d:\n",reg_if);

        return null;

    }


    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    @Override
    public String visit(WhileStatement n, String[] scope) throws Exception {
        //System.out.println("IfStatement   -- ");
        int reg_while=Main.whil++;
        Main.line_to_write=Main.line_to_write+String.format("\tbr label %%into_while_%d\n\tinto_while_%d:\n",reg_while,reg_while);

        String e1=n.f2.accept(this, scope);
        e1=Main.load(e1,"i1",scope);
        e1=Main.getdata(e1,"i1",scope,"use");
        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %s, label %%loop_while_%d, label %%out_while_%d\n\tloop_while_%d:\n",e1,reg_while,reg_while,reg_while);
        n.f4.accept(this, scope);
        Main.line_to_write=Main.line_to_write+String.format("\tbr label %%into_while_%d\n\tout_while_%d:\n",reg_while,reg_while);
        return null;

    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    @Override
    public String visit(PrintStatement n, String[] scope) throws Exception {
        //System.out.println("PrintStatement   -- ");
        String temp=n.f2.accept(this, scope);
        //System.out.println("PrintStatement   -- after accept");
        temp=Main.load(temp,"i32",scope);
        temp=Main.getdata(temp,"i32",scope,"use");
        Main.line_to_write=Main.line_to_write+String.format("\tcall void (i32) @print_int(i32 %s)\n",temp);
        return null;
    }
    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    @Override
    public String visit(Expression n, String[] scope) throws Exception {
        //System.out.println("expression   -- ");
        

        String temp=n.f0.accept(this, scope);
        //System.out.println("expression   -- "+ temp);
        return temp;
    }
    
    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    @Override
    public String visit(AndExpression n, String[] scope) throws Exception {
        //System.out.println("andexpression   -- ");
        String cl1= n.f0.accept(this, scope);
        String cl2= n.f2.accept(this, scope);

        cl1=Main.load(cl1,"i1",scope);
        cl1=Main.getdata(cl1,"i1",scope,"use");

        int reg_exp_res=Main.exp_res++;
        //int reg_load=Main.reg++;
        int reg_phi=Main.reg++;

        
        


        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %s, label %%exp_res_sc_%d, label %%exp_res_%d\n",cl1,reg_exp_res,reg_exp_res);
        Main.line_to_write=Main.line_to_write+String.format("\texp_res_%d:\n\tbr label %%exp_res_go_%d\n",reg_exp_res,reg_exp_res);
        Main.line_to_write=Main.line_to_write+String.format("\texp_res_sc_%d:\n",reg_exp_res);

        cl2=Main.load(cl2,"i1",scope);
        cl2=Main.getdata(cl2,"i1",scope,"use");

        Main.line_to_write=Main.line_to_write+String.format("\tbr label %%exp_res_go2_%d\n",reg_exp_res);
        Main.line_to_write=Main.line_to_write+String.format("\texp_res_go2_%d:\n\tbr label %%exp_res_go_%d\n",reg_exp_res,reg_exp_res);
        Main.line_to_write=Main.line_to_write+String.format("\texp_res_go_%d:\n\t%%_%d = phi i1  [ 0, %%exp_res_%d ], [ %s, %%exp_res_go2_%d ]\n",reg_exp_res,reg_phi,reg_exp_res,cl2,reg_exp_res);
        return "%_"+reg_phi;
        
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(CompareExpression n, String[] scope) throws Exception {
        String cl1= n.f0.accept(this, scope);
        String cl2= n.f2.accept(this, scope);
        cl1=Main.load(cl1,"i32",scope);
        cl1=Main.getdata(cl1,"i32",scope,"use");
        cl2=Main.load(cl2,"i32",scope);
        cl2=Main.getdata(cl2,"i32",scope,"use");
        int new_reg=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp slt i32 %s, %s\n",new_reg,cl1,cl2);
        
        return "%_"+Integer.toString(new_reg);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(PlusExpression n, String[] scope) throws Exception {
        String cl1= n.f0.accept(this, scope);
        String cl2= n.f2.accept(this, scope);
        cl1=Main.load(cl1,"i32",scope);
        cl1=Main.getdata(cl1,"i32",scope,"use");
        cl2=Main.load(cl2,"i32",scope);
        cl2=Main.getdata(cl2,"i32",scope,"use");
        int new_reg=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = add i32 %s, %s\n",new_reg,cl1,cl2);
        
        return "%_"+Integer.toString(new_reg);
    }

    @Override
    public String visit(MinusExpression n, String[] scope) throws Exception {
        String cl1= n.f0.accept(this, scope);
        String cl2= n.f2.accept(this, scope);
        cl1=Main.load(cl1,"i32",scope);
        cl1=Main.getdata(cl1,"i32",scope,"use");
        cl2=Main.load(cl2,"i32",scope);
        cl2=Main.getdata(cl2,"i32",scope,"use");
        int new_reg=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = sub i32 %s, %s\n",new_reg,cl1,cl2);
        
        return "%_"+Integer.toString(new_reg);
    }

    @Override
    public String visit(TimesExpression n, String[] scope) throws Exception {
        //System.out.println("mulexpression   --");
        String cl1= n.f0.accept(this, scope);
        String cl2= n.f2.accept(this, scope);
        //System.out.println("mulexpression   -- after cl1= "+cl1);
        cl1=Main.load(cl1,"i32",scope);
        cl1=Main.getdata(cl1,"i32",scope,"use");
        cl2=Main.load(cl2,"i32",scope);
        cl2=Main.getdata(cl2,"i32",scope,"use");

        int new_reg=Main.reg++;
        //System.out.println("mulexpression   --cl2= "+cl2);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = mul i32 %s, %s\n",new_reg,cl1,cl2);
        
        return "%_"+Integer.toString(new_reg);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public String visit(ArrayLookup n, String[] scope) throws Exception {
        //System.out.println("Start of arraylokkup -- "+ scope[1]);
        String e1= n.f0.accept(this, scope);
        String e2= n.f2.accept(this, scope);
        /*if(e1.charAt(1)='_'){
            e1=Main.getdata(e1,"i32*",scope,"use");
        }
        else{
            e1=Main.getdata(e1,"i32*",scope,"use");
        }*/
        
        e1=Main.load(e1,"i32*",scope);
        e1=Main.getdata(e1,"i32*",scope,"use");
        //System.out.println("After accepts of arraylokkup -- "+ e1+" "+e2);
        //int reg_add=Main.reg++;
        int reg_size=Main.reg++;
        int reg_cmp_nz=Main.reg++;
        int reg_cmp_oob=Main.reg++;
        int reg_cmp=Main.reg++;
        int label_oob_ok=Main.oob++;
        int label_oob_error=label_oob_ok;
        int reg_offset=Main.reg++;
        int reg_element=Main.reg++;
        int reg_load=Main.reg++;
        
        
        //Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32*, i32** %s\n",reg_add,e1);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32, i32* %s\n",reg_size,e1);

        /*if(e2.charAt(1)=='%'){
            int reg_temp=Main.reg++;
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32, i32* %s\n",reg_temp,e2);
            e2=String.format("%%_%d",reg_temp);
        }*/
        e2=Main.load(e2,"i32",scope);
        e2=Main.getdata(e2,"i32",scope,"use");
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp sge i32 %s, 0\n",reg_cmp_nz,e2);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp slt i32 %s, %%_%d\n", reg_cmp_oob,e2,reg_size);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = and i1 %%_%d, %%_%d\n",reg_cmp,reg_cmp_nz,reg_cmp_oob);
        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %%_%d, label %%oob_ok_%d, label %%oob_err_%d\n",reg_cmp,label_oob_ok,label_oob_error);
        Main.line_to_write=Main.line_to_write+String.format("\toob_err_%d:\n\tcall void @throw_oob()\n\tbr label %%oob_ok_%d\n\toob_ok_%d:\n",label_oob_ok,label_oob_ok,label_oob_ok);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = add i32 1, %s\n",reg_offset,e2);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = getelementptr i32, i32* %s, i32 %%_%d\n",reg_element,e1,reg_offset);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32, i32* %%_%d\n\n",reg_load,reg_element);
        //Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i32 %_6 to i8***\n");
        //System.out.println(Main.line_to_write);
        return "%_"+Integer.toString(reg_load);
    }


    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    @Override
    public String visit(ArrayLength n, String[] scope) throws Exception {
        String e1= n.f0.accept(this, scope);
        
        e1=Main.load(e1,"i32*",scope);
        e1=Main.getdata(e1,"i32*",scope,"use");
        

        int reg_load=Main.reg++;
        //int reg_size=Main.reg++;

        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32, i32* %s\n",reg_load,e1);
        //Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i32, i32* %%_%d\n",reg_size,reg_load);

        return String.format("%%_%d",reg_load);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    @Override
    public String visit(MessageSend n, String[] scope) throws Exception {
        //System.out.println("Message send -- "+ scope[1]);
        String e1= n.f0.accept(this, scope);
        String id= n.f2.accept(this, scope);
        
        String e1_=e1.substring(1);
        String id_type=Main.existsinscopes(e1_,scope);
        String actual_type;
        if(id_type==null){
            
            actual_type=Main.keep_new.get(e1);
            //System.out.println("Message send -- idtype=null and "+ actual_type);
        }
        else{
            String[] actual_id=id_type.split("\\s++");
            actual_type=actual_id[1];
            //System.out.println("Message send -- idtype!=null and"+ actual_type);
        }
        

        id=id.substring(1);
        
        e1=Main.getdata(e1,"i8*",scope,"use");
        //System.out.println("Message send -- ei= "+ e1+" id= "+ id);
        String elist= n.f4.present() ? n.f4.accept(this, scope): null;
        //System.out.println("Message send -- elist= "+ elist);
        //int reg_object=Main.reg++;
        int reg_bitcast=Main.reg++;
        int reg_load=Main.reg++;
        int reg_elem=Main.reg++;
        int reg_func=Main.reg++;
        int reg_bitcast_1=Main.reg++;
        int reg_call=Main.reg++;

        
        
        
        
        //System.out.println("Message send -- actual_id= "+ actual_type);
        LinkedHashMap<String,String> vtable=Main.vtables.get(actual_type);
        String[] func_offset=vtable.get(id).split("\\s++");
        
        //System.out.println("Message send --  at least here");
        /*if(e1_.charAt(0)=='_'){
            System.out.println("Message send --  in if");
            //Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i8**, i8*** %%_%d\n",reg_load,reg_bitcast);
            reg_object=Integer.valueOf(e1_.substring(1));
        }
        else{
            Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i8*, i8** %s\n",reg_object,e1);
        }*/
        e1=Main.load(e1,"i8*",scope);
        e1=Main.getdata(e1,"i8*",scope,"use");
        //System.out.println("Message send --  at least here");
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i8* %s to i8***\n",reg_bitcast,e1); 
        
        
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i8**, i8*** %%_%d\n",reg_load,reg_bitcast);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = getelementptr i8*, i8** %%_%d, i32 %s\n",reg_elem,reg_load,func_offset[1]);
        
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = load i8*, i8** %%_%d\n",reg_func,reg_elem);
        
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i8* %%_%d to ",reg_bitcast_1,reg_func);

        LinkedHashMap<String, String> method_class=Main.smap.get("Method Class "+func_offset[0]+" "+id);
        //System.out.println("At least here: "+"Method Class "+func_offset[0]+" "+id);
        String type=method_class.get("Type ");
        //System.out.println("Message send --  at least here");
        Main.line_to_write=Main.line_to_write+Main.gettype(type)+" (i8*";
        
        String arguments_=method_class.get("Arg ");
        String[] arguments=arguments_.split("\\s+");
        
        //System.out.println("Message send --  at least here");
        String next_line=String.format("\t%%_%d = call %s %%_%d(i8* %s",reg_call,Main.gettype(type),reg_bitcast_1,e1);
        //System.out.println("Message send --  at least here");
        //System.out.println(arguments_);
        if(!arguments_.isEmpty()){
            //int count=0;
            String[] elist_split=elist.split("\\s+");
            for(String a:arguments){
                if(!a.equals("Class")){
                    //System.out.println("\n"+a+" " + Main.gettype(a)+"\n");
                    Main.line_to_write=Main.line_to_write+","+Main.gettype(a);
                    //elist_split[count]=Main.load(elist_split[count],Main.gettype(a),scope,Main.special_line);
                    //elist_split[count]=Main.getdata(elist_split[count],Main.gettype(a),scope,"use",Main.special_line);
                
                    //next_line=next_line+", "+ Main.gettype(a)+" "+elist_split[count];
                    //System.out.println(next_line);
                    //count=count+1;
                }
            }
        }
        Main.line_to_write=Main.line_to_write+")*\n";
        if(!arguments_.isEmpty()){
            int count=0;
            String[] elist_split=elist.split("\\s+");
            for(String a:arguments){
                if(!a.equals("Class")){
                    //System.out.println("\n"+a+" " + Main.gettype(a)+"\n");
                    //Main.line_to_write=Main.line_to_write+","+Main.gettype(a);
                    elist_split[count]=Main.load(elist_split[count],Main.gettype(a),scope);
                    elist_split[count]=Main.getdata(elist_split[count],Main.gettype(a),scope,"use");
                
                    next_line=next_line+", "+ Main.gettype(a)+" "+elist_split[count];
                    //System.out.println(next_line);
                    count=count+1;
                }
            }
        }
        next_line=next_line+")\n";
        Main.line_to_write=Main.line_to_write+next_line;
        if(Main.gettype(type).equals("i8*")){
            Main.keep_new.put(String.format("%%_%d",reg_call),actual_type);
        }
        
        return "%_"+reg_call;
    }
    
    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    @Override
    public String visit(ExpressionList n, String[] scope) throws Exception {
        String ret = n.f0.accept(this,scope);

        if (n.f1 != null) {
            ret += n.f1.accept(this, scope);
        }

        return ret;

    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(ExpressionTerm n,String[] scope) throws Exception {
        //System.out.println("formalterm   -- ");
        return n.f1.accept(this, scope);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(ExpressionTail n, String[] scope) throws Exception {
        //System.out.println("formaltail   -- ");
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += " " + node.accept(this, scope);
        }

        return ret;
    }

    /**
    * f0 -> IntegerLiteral()                int
    *       | TrueLiteral()                 boolean
    *       | FalseLiteral()                boolean
    *       | Identifier()                  <blah>, primary expression will change it to <type>
    *       | ThisExpression()              <type>
    *       | ArrayAllocationExpression()   int[]/boolean[] allocation
    *       | AllocationExpression()        <type>
    *       | BracketExpression()           same thing as primaryExpression except the allocations
    */
    @Override
    public String visit(PrimaryExpression n, String[] scope) throws Exception {
        
        String temp=n.f0.accept(this,scope);
        //System.out.println("Primaryexpression   -- "+ temp);
        
        return temp;
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    @Override
    public String visit(IntegerLiteral n, String[] scope) throws Exception {
        String num=n.f0.toString();
        return num+" ";
    }

    /**
    * f0 -> "true"
    */
    @Override
    public String visit(TrueLiteral n, String[] scope) throws Exception {
        return "1";
    }

    /**
    * f0 -> "false"
    */
    @Override
    public String visit(FalseLiteral n, String[] scope) throws Exception {
        return "0";
    }

    

    /**
    * f0 -> "this"
    */
    @Override
    public String visit(ThisExpression n, String[] scope) throws Exception {
        
        /*String actual_id=(scope[0].split("\\s++"))[1];
        int reg_alloc=Main.reg++;
        LinkedHashMap<String,Integer> vartable_id= Main.vartables.get(actual_id);
        LinkedHashMap<String,String> vtable_id=Main.vtables.get(actual_id);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = call i8* @calloc(i32 1, i32 %d)\n",reg_alloc,vartable_id.get("Sum ")+8);
        int reg_bit=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i8* %%_%d to i8***\n",reg_bit,reg_alloc);
        int reg_elem=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = getelementptr [%d x i8*], [%d x i8*]* @.%s_vtable, i32 0, i32 0\n",reg_elem,vtable_id.size(),vtable_id.size(),actual_id);
        //int reg_store=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\tstore i8** %%_%d, i8*** %%_%d\n",reg_elem,reg_bit);
        Main.keep_new.put(String.format("%%_%d",reg_alloc),actual_id);*/
        String actual_id=(scope[0].split("\\s++"))[1];
        
        Main.keep_new.put("%this",actual_id);
        return "%this";
    }

    /**
    * f0 -> BooleanArrayAllocationExpression()
    *       | IntegerArrayAllocationExpression()
    */
    @Override
    public String visit(ArrayAllocationExpression n, String[] scope) throws Exception {
        return n.f0.accept(this,scope);
    }

    /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(BooleanArrayAllocationExpression n, String[] scope) throws Exception {
        //System.out.println("IntegerArrayAllocationExpression");
        String reg_exp=n.f3.accept(this,scope);
        int reg_size=Main.reg++;
        reg_exp=Main.load(reg_exp,"i32",scope);
        reg_exp=Main.getdata(reg_exp,"i32",scope,"use");
        
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = add i32 1, %s\n",reg_size,reg_exp);

        int reg_cmp=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp sge i32 %%_%d, 1\n",reg_cmp,reg_size);
        int reg_nsz=Main.nsz++;
        //System.out.println("Here");
        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %%_%d, label %%nsz_ok_%d, label %%nsz_err_%d\n",reg_cmp,reg_nsz,reg_nsz);
        Main.line_to_write=Main.line_to_write+String.format("\tnsz_err_%d:\n\tcall void @throw_nsz()\n\tbr label %%nsz_ok_%d\n",reg_nsz,reg_nsz);
        Main.line_to_write=Main.line_to_write+String.format("\tnsz_ok_%d:\n",reg_nsz);
        //System.out.println("Here");
        int reg_alloc=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = call i8* @calloc(i32 %%_%d, i32 4)\n",reg_alloc, reg_size);
        int reg_cast=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i8* %%_%d to i32*\n",reg_cast,reg_alloc);
        Main.line_to_write=Main.line_to_write+String.format("\tstore i32 %s, i32* %%_%d\n\n",reg_exp,reg_cast);
        //System.out.println(Main.line_to_write);
        System.out.println("IntegerArrayAllocationExpression --end");
        return String.format("%%_%d",reg_cast);
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(IntegerArrayAllocationExpression n, String[] scope) throws Exception {
        //System.out.println("IntegerArrayAllocationExpression");
        String reg_exp=n.f3.accept(this,scope);
        int reg_size=Main.reg++;
        reg_exp=Main.load(reg_exp,"i32",scope);
        reg_exp=Main.getdata(reg_exp,"i32",scope,"use");
        
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = add i32 1, %s\n",reg_size,reg_exp);

        int reg_cmp=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = icmp sge i32 %%_%d, 1\n",reg_cmp,reg_size);
        int reg_nsz=Main.nsz++;
        //System.out.println("Here");
        Main.line_to_write=Main.line_to_write+String.format("\tbr i1 %%_%d, label %%nsz_ok_%d, label %%nsz_err_%d\n",reg_cmp,reg_nsz,reg_nsz);
        Main.line_to_write=Main.line_to_write+String.format("\tnsz_err_%d:\n\tcall void @throw_nsz()\n\tbr label %%nsz_ok_%d\n",reg_nsz,reg_nsz);
        Main.line_to_write=Main.line_to_write+String.format("\tnsz_ok_%d:\n",reg_nsz);
        //System.out.println("Here");
        int reg_alloc=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = call i8* @calloc(i32 %%_%d, i32 4)\n",reg_alloc, reg_size);
        int reg_cast=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i8* %%_%d to i32*\n",reg_cast,reg_alloc);
        Main.line_to_write=Main.line_to_write+String.format("\tstore i32 %s, i32* %%_%d\n\n",reg_exp,reg_cast);
        //System.out.println(Main.line_to_write);
        //System.out.println("IntegerArrayAllocationExpression --end");
        
        return String.format("%%_%d",reg_cast);
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    @Override
    public String visit(AllocationExpression n, String[] scope) throws Exception {
        //System.out.println("AllocationExpression");
        String id= n.f1.accept(this,scope);
        String actual_id=id.substring(1);
        int reg_alloc=Main.reg++;
        LinkedHashMap<String,Integer> vartable_id= Main.vartables.get(actual_id);
        LinkedHashMap<String,String> vtable_id=Main.vtables.get(actual_id);
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = call i8* @calloc(i32 1, i32 %d)\n",reg_alloc,vartable_id.get("Sum ")+8);
        int reg_bit=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = bitcast i8* %%_%d to i8***\n",reg_bit,reg_alloc);
        int reg_elem=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = getelementptr [%d x i8*], [%d x i8*]* @.%s_vtable, i32 0, i32 0\n",reg_elem,vtable_id.size(),vtable_id.size(),actual_id);
        //int reg_store=Main.reg++;
        Main.line_to_write=Main.line_to_write+String.format("\tstore i8** %%_%d, i8*** %%_%d\n",reg_elem,reg_bit);
        
        //System.out.println(Main.line_to_write);
        Main.keep_new.put(String.format("%%_%d",reg_alloc),actual_id);
        return "%_"+Integer.toString(reg_alloc);
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    @Override
    public String visit(NotExpression n, String[] scope) throws Exception {
        String reg_=n.f1.accept(this,scope);
        int new_reg=Main.reg++;
        reg_=Main.load(reg_,"i1",scope);
        reg_=Main.getdata(reg_,"i1",scope,"use");
        Main.line_to_write=Main.line_to_write+String.format("\t%%_%d = xor i1 1, %s\n",new_reg,reg_);
        return "%_"+Integer.toString(new_reg);
    }

    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    @Override
    public String visit(Clause n, String[] scope) throws Exception {
        return n.f0.accept(this,scope);
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, String[] scope) throws Exception {
        return n.f1.accept(this,scope);
    }


    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String[] scope) throws Exception {
        //System.out.println("formalParameter   -- ");
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n,String[] scope) throws Exception {
        //System.out.println("formalterm   -- ");
        return n.f1.accept(this, null);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String[] scope) throws Exception {
        //System.out.println("formaltail   -- ");
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += " " + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String[] scope) throws Exception{
        //System.out.println("fromalParameter   -- ");
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
    }

    @Override
    public String visit(BooleanArrayType n, String[] scope) {
        return "boolean*";
    }

    @Override
    public String visit(IntegerArrayType n, String[] scope) {
        return "int*";
    }

    public String visit(BooleanType n, String[] scope) {
        return "boolean";
    }

    public String visit(IntegerType n, String[] scope) {
        return "int";
    }

    @Override
    public String visit(Identifier n, String[] scope) {
        //System.out.println("Id: "+n.f0.toString());
        
        return "%"+n.f0.toString();
    }    
}