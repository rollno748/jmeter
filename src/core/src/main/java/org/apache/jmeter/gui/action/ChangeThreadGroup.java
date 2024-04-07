/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.gui.action;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;

/**
 * Allows to change ThreadGroup
 */
@AutoService(Command.class)
public class ChangeThreadGroup extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(ChangeThreadGroup.class);

    private static final Set<String> commands = new HashSet<>();

    static {
        commands.add(ActionNames.CHANGE_THREAD_GROUP);
    }

    public ChangeThreadGroup() {
    }

    @Override
    public void doAction(ActionEvent e) {
        String name = ((Component) e.getSource()).getName();
        GuiPackage guiPackage = GuiPackage.getInstance();
        JMeterTreeNode currentNode = guiPackage.getTreeListener().getCurrentNode();
        if (!(currentNode.getUserObject() instanceof ThreadGroup)) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            guiPackage.updateCurrentNode();
            ThreadGroup threadGroup = (ThreadGroup) guiPackage.createTestElement(name);
            changeThreadGroup(threadGroup, guiPackage, currentNode);
        } catch (Exception err) {
            Toolkit.getDefaultToolkit().beep();
            log.error("Failed to change thread group", err);
        }

    }

    @Override
    public Set<String> getActionNames() { return commands; }

    private static void changeThreadGroup(ThreadGroup threadGroup, GuiPackage guiPackage, JMeterTreeNode currentNode) {
        ThreadGroup currentThreadGroup = (ThreadGroup) currentNode.getUserObject();
        JMeterGUIComponent currentGui = guiPackage.getCurrentGui();
        String defaultName = JMeterUtils.getResString(currentGui.getStaticLabel());

        if(StringUtils.isNotBlank(currentThreadGroup.getName())
                && !currentThreadGroup.getName().equals(defaultName)){
            threadGroup.setName(currentThreadGroup.getName());
        }

        JMeterTreeModel treeModel = guiPackage.getTreeModel();
        JMeterTreeNode newNode = new JMeterTreeNode(threadGroup, treeModel);
        JMeterTreeNode parentNode = (JMeterTreeNode) currentNode.getParent();
        int index = parentNode.getIndex(currentNode);
        treeModel.insertNodeInto(newNode, parentNode, index);
        int childCount = currentNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            JMeterTreeNode node = (JMeterTreeNode) currentNode.getChildAt(0);
            treeModel.removeNodeFromParent(node);
            treeModel.insertNodeInto(node, newNode, newNode.getChildCount());
        }
        treeModel.removeNodeFromParent(currentNode);
        treeModel.nodeStructureChanged(parentNode);
        guiPackage.getTreeModel().reload();
        guiPackage.updateCurrentGui();
        // select the node
        TreeNode[] nodes = treeModel.getPathToRoot(newNode);
        JTree tree = guiPackage.getTreeListener().getJTree();
        tree.setSelectionPath(new TreePath(nodes));
    }
}
