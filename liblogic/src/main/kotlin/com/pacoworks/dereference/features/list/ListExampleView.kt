/*
 * Copyright (c) pakoito 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacoworks.dereference.features.list

import com.pacoworks.dereference.architecture.ui.BoundView
import com.pacoworks.dereference.core.functional.None
import com.pacoworks.dereference.features.list.model.EditMode
import org.javatuples.Pair
import rx.Observable

/**
 * Composite interface including all inputs and outputs for this feature
 */
interface ListExampleView : ListExampleInputView, ListExampleOutputView

/**
 * Interface representing all UI changing side-effects that can be applied to this screen.
 *
 * It extends [BoundView] to provide generic bindings between the view and the state
 */
interface ListExampleInputView : BoundView {
    fun updateElements(elements: List<String>): Unit
    fun updateSelected(selected: Set<String>): Unit
    fun updateEditMode(editMode: EditMode): Unit
}

/**
 * Interface representing all signal for user interaction this screen provides
 */
interface ListExampleOutputView {
    fun listClicks(): Observable<Pair<Int, String>>

    fun listLongClicks(): Observable<Pair<Int, String>>

    fun addClick(): Observable<None>

    fun deleteClick(): Observable<None>

}

