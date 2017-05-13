package ru.l2gw.gameserver.model.entity.fieldcycle;

/**
 * @author: rage
 * @date: 11.12.11 18:31
 */
public interface IFieldCycleMaker
{
	public void onFieldCycleChanged(int fieldId, int oldStep, int newStep);
	
	public void onFieldCycleExpired(int fieldId, int oldStep, int newStep);
}
