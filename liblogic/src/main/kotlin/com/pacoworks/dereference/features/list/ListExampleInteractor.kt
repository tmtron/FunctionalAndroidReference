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

import com.pacoworks.dereference.architecture.ui.StateHolder
import com.pacoworks.dereference.core.functional.None
import com.pacoworks.dereference.features.list.model.EditMode
import com.pacoworks.dereference.features.list.model.createEditModeDelete
import com.pacoworks.dereference.features.list.model.createEditModeNormal
import com.pacoworks.rxcomprehensions.RxComprehensions.doFM
import com.pacoworks.rxcomprehensions.RxComprehensions.doSM
import com.pacoworks.rxtuples.RxTuples
import org.javatuples.Pair
import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Binds the state of this use case to a [com.pacoworks.dereference.architecture.ui.BoundView]
 *
 * @see [com.pacoworks.dereference.architecture.ui.bind]
 */
fun bindListExample(viewInput: ListExampleInputView, state: ListExampleState) {
    viewInput.createBinder<List<String>>().call(state.elements, { viewInput.updateElements(it) })
    viewInput.createBinder<Set<String>>().call(state.selected, { viewInput.updateSelected(it) })
    viewInput.createBinder<EditMode>().call(state.editMode, { viewInput.updateEditMode(it) })
}

/**
 * Subscribes all use cases in the file
 */
fun subscribeListExampleInteractor(viewOutput: ListExampleOutputView, state: ListExampleState): Subscription =
        CompositeSubscription(
                handleAdd(state.elements, viewOutput.addClick()),
                handleEnterEditState(viewOutput.listLongClicks(), state.editMode),
                handleExitEditState(viewOutput.deleteClick(), state.editMode),
                handleOnCommitDelete(state.editMode, state.elements, state.selected),
                handleEnterEditStateSelectLongClickedItem(state.editMode, state.selected),
                handleSelect(state.editMode, state.selected, viewOutput.listClicks()))

fun handleAdd(elementsState: StateHolder<List<String>>, addClick: Observable<None>): Subscription =
        // switch map -
        // "recursive" state? we use elementState as source and also subscribe to it
        doSM(
                /* For the current list of elements */
                { elementsState },
                /* wait until the user clicks add */
                { addClick.first() },
                { elements, click ->
                    /* Insert random number at end of the list */
                    Observable.just(elements.plus((Math.abs(Random().nextInt()) + elements.size).toString()))
                }
        )
                // push the new state into element state
                .subscribe(elementsState)

fun handleEnterEditState(listLongClicks: Observable<Pair<Int, String>>, editMode: StateHolder<EditMode>): Subscription =
        // flatMap
        doFM(
                /* When the user clicks on the list with a long press */
                { listLongClicks },
                /* get the most recent edit mode */
                { editMode.first() },
                { click, editModeUnion ->
                    editModeUnion.join(
                        /* when the current state is normal (Add-button), then change to delete mode and pass
                           the list-id of the item that was pressed
                         */
                        { normal -> Observable.just(createEditModeDelete(click.value1)) },
                        /* Else ignore - we are already in edit state (Delete-button is shown) */
                        { delete -> Observable.empty<EditMode>() }) }
        ).subscribe(editMode)

fun handleExitEditState(deleteClick: Observable<None>, editMode: StateHolder<EditMode>): Subscription =
        doFM(
                /* When the user clicks on the delete button*/
                { deleteClick },
                /* get the edit-mode */
                { editMode.first() },
                { click, editMode ->
                    editMode.join(
                            /* we are not in edit mode (e.g. Add-button is shown) */
                            { normal -> Observable.empty<EditMode>() },
                            /* we are in edit mode - switch to normal (show Add-button) */
                            { delete -> Observable.just(createEditModeNormal()) })
                }
        ).subscribe(editMode)

fun handleEnterEditStateSelectLongClickedItem(editMode: StateHolder<EditMode>, selected: StateHolder<Set<String>>): Subscription =
        /* When edit mode changes to deleted select the current item (that was long-clicked) */
        editMode
                .flatMap { it.join(
                          {normal -> Observable.empty<Set<String>>()}
                        , {deleted -> Observable.just(setOf(deleted.id))}
                ) }
                .subscribe(selected)

fun handleOnCommitDelete(editMode: StateHolder<EditMode>, elementsState: StateHolder<List<String>>, selected: StateHolder<Set<String>>): Subscription =
        doSM(
                /* When the mode changes from delete to normal */
                { editMode.filter { it.join({ normal -> true }, { delete -> false }) } },
                /* Get all elements, and the set of selected ones */
                { Observable.zip(elementsState, selected, RxTuples.toPair<List<String>, Set<String>>()).first() },
                /* Delete selected elements */
                { exitEditMode, statePair ->
                    Observable.from(statePair.value0).filter { !statePair.value1.contains(it) }.toList()
                }
        ).doOnNext { selected.call(setOf()) }
                .subscribe(elementsState)

/*
 * you can only select items in DELETE mode
 */
fun handleSelect(editMode: StateHolder<EditMode>, selected: StateHolder<Set<String>>, listClicks: Observable<Pair<Int, String>>): Subscription =
        /* When the edit mode is delete */
        editMode.
                switchMap {
                    it.join(
                            /* Ignore clicks if not editing */
                            { normal -> Observable.empty<String>() },
                            /* Else pass them through */
                            { delete -> listClicks.map { it.value1 } })
                }
                .flatMap { value ->
                    /* toggle selection state */
                    selected.first().map {
                        if (it.contains(value)) {
                            it.minus(value)
                        } else {
                            it.plus(value)
                        }
                    }
                }.subscribe(selected)