/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2014 Matthias Wimmer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.wimmerinformatik.sbfb.data;

/**
 * POJO that contains a grouping of questsions.
 *
 * We should the different topics we have questions for on the start screen of
 * the app.
 *
 * @author Matthias Wimmer
 */
public class Topic {
	/**
	 * ID of the topic.
	 */
	private int id;

	/**
	 * Ordering when showing the topics.
	 *
	 * Show {@link Topic} with smaller index before others with bigger index.
	 */
	private int index;

	/**
	 * How to name the topic.
	 */
	private String name;

	/**
	 * Getter for the ID of the Topic.
	 *
	 * @return the ID of the topic
	 */
	public int getId() {
		return id;
	}

	/**
	 * Setter for the ID of the Topic.
	 *
	 * @param id the ID to set
	 */
	public void setId(final int id) {
		this.id = id;
	}

	/**
	 * Getter for the ordering index.
	 *
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Setter for the ordering index.
	 *
	 * @param index the index value (smaller index before bigger one)
	 */
	public void setIndex(final int index) {
		this.index = index;
	}

	/**
	 * Get the name of the topic.
	 *
	 * @return name of the topic
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the topic.
	 *
	 * @param name the name of the topic
	 */
	public void setName(final String name) {
		this.name = name;
	}
}
