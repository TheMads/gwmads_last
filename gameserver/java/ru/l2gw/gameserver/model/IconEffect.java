package ru.l2gw.gameserver.model;

/** 
 * @author    ALF 
 * @data    07.02.2012 
 * Класс для иконки эффекта 
 */ 
public class IconEffect  
{ 
    private final int _skillId; 
    private final int _level; 
    private final int _duration; 

    public IconEffect(int skillId, int level, int duration) 
    { 
        _skillId = skillId; 
        _level = level; 
        _duration = duration; 
    } 

    public int getSkillId() { 
        return _skillId; 
    } 

    public int getLevel() { 
        return _level; 
    } 

    public int getDuration() { 
        return _duration; 
    } 
}  
