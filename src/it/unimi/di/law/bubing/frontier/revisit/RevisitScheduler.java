/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier.revisit;

import it.unimi.di.law.bubing.frontier.PathQueryState;
import it.unimi.di.law.bubing.util.FetchData;

/**
 *
 * @author karel
 */
public interface RevisitScheduler {

	public PathQueryState schedule( FetchData fetchData, PathQueryState pathQuery, boolean modified );
}
