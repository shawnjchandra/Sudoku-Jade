/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sudokumodel.Models;

/**
 *
 * @author Apple's 1
 */

public class Box {
  private int Value;
  private int agentType;
  
  public Box (int v){
      this.Value = v;
  }

  public Box(int v, int x, int y){
    this.Value = v;

  }
  
  public Box(int v, int a){
      this.Value = v;
      this.agentType = a;
  }

  void printBox(){
    System.out.println ("("+this.Value+")");
  }

  void printValue (){
    System.out.println (this.Value);
  }


  public int getValue(){
      return this.Value;
  }
  
  public void setValue(int x){
      this.Value = x;
  }

  int getAgentType(){
      return this.agentType;
  }
  void setAgentType (int i){
      this.agentType = i;
  }
}
