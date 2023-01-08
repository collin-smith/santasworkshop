package com.myorg.dynamodb;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

public class LetterBuilder {
  private Letter letter;
  private int inserted;
  private DynamoDBMapper mapper;
  private String log = "";
    
  public LetterBuilder(DynamoDBMapper mapper, DynamoDBMapperConfig mapperConfig) 
  {
    this.mapper = mapper;
    letter = new Letter();
  }
    
  public void setLetter(Letter letter)
  {
    this.letter.setPK(letter.getPK());
    this.letter.setSK(letter.getSK());
    this.letter.setString1(letter.getString1());
    this.letter.setString2(letter.getString2());
    this.letter.setString3(letter.getString3());
    this.letter.setDouble1(letter.getDouble1());
    this.letter.setDate1(letter.getDate1());   
  }

  public DynamoDBMapper getMapper() 
  {
    return this.mapper;
  }

  public LetterBuilder PK(String PK) 
  {
    letter.setPK(PK);
    return this;
  }

  public LetterBuilder SK(String SK) 
  {
    letter.setSK(SK);
    return this;
  }

  public LetterBuilder string1(String string1) 
  {
    letter.setString1(string1);
    return this;
  }
  
  public LetterBuilder string2(String string2) 
  {
    letter.setString2(string2);
    return this;
  }
  
  public LetterBuilder string3(String string3) 
  {
    letter.setString3(string3);
    return this;
  }

  public LetterBuilder double1(double double1) 
  {
    letter.setDouble1(double1);
    return this;
  }

  public LetterBuilder date1(Date date1) 
  {
    letter.setDate1(date1);
    return this;
  }
    
  public int count() 
  {
    return inserted;
  }
  
  public String getLog()
  {
    return this.log;
  }

  public void save() 
  {
    mapper.save(letter);
    log+=" Saved letter "+letter.getPK()+","+letter.getSK()+ ",";
    letter = new Letter();
    inserted+=1;
  }
}