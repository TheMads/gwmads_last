package ru.l2gw.extensions.maintenance;

/**
 * @author rage
 * @date 12.08.2009 12:45:34
 */
public abstract class MaintenanceTask
{
	public abstract boolean doTask(String params);

	public abstract String getLastResult();

	public abstract void addTask(String params);
}
