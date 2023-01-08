
package com.myorg.dynamodb;

import java.util.Date;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="Letter")
public class Letter 
{

  private String PK;
  private String SK;
  private String string1;
  private String string2;
  private String string3;
  private Double double1;
  private Date date1;
  
  //Maps a class property to the partition key of the table.
  @DynamoDBHashKey(attributeName="PK")
  public String getPK() { return PK; }
  public void setPK(String state) {this.PK = state; }

  //Maps a class property to the sort key of the table.
  @DynamoDBRangeKey(attributeName="SK")
  public String getSK() {return SK; }
  public void setSK(String SK) { this.SK = SK; }

  //Maps a class property to the partition key of the global secondary index String1GSIIndex
  @DynamoDBIndexHashKey(globalSecondaryIndexName = "String1GSIIndex", attributeName="string1")
  public String getString1() { return this.string1 ; }
  public void setString1(String string1) { this.string1 = string1; }

  @DynamoDBAttribute(attributeName="string2")
  public String getString2() { return this.string2 ; }
  public void setString2(String string2) { this.string2 = string2; }

  @DynamoDBAttribute(attributeName="string3")
  public String getString3() { return this.string3 ; }
  public void setString3(String string3) { this.string3 = string3; }

  @DynamoDBAttribute(attributeName="double1")
  public double getDouble1() { return this.double1 ; }
  public void setDouble1(double double1) { this.double1 = double1; }

  @DynamoDBAttribute(attributeName="date1")
  public Date getDate1() { return this.date1 ; }
  public void setDate1(Date date1) { this.date1 = date1; }

  public String toString()
  {
    String val = "Letter(";
    val += this.PK+",";
    val += this.SK+")";
    val += "string1:"+this.string1+",";
    val += "string2:"+this.string2+",";
    val += "string3:"+this.string3+",";
    val += "double1:"+this.double1+",";
    val += "date1:"+this.date1+".";
    return val;
  }
  
}
