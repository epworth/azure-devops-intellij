package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ArrayOfWorkingFolder;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.WorkingFolder;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.WorkingFolderType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Workspace;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class WorkspaceInfo {

  // TODO: do we need owner name and computer name here?

  private final ServerInfo myServerInfo;
  private final String myOwnerName;
  private final String myComputer;

  private String myOriginalName;
  private String myComment;
  private Calendar myTimestamp;
  //private boolean myWorkingFoldersCached;
  private String myModifiedName;

  private List<WorkingFolderInfo> myWorkingFoldersInfos = new ArrayList<WorkingFolderInfo>();

  public WorkspaceInfo(final ServerInfo serverInfo, final String owner, final String computer) {
    if (serverInfo == null) {
      throw new IllegalArgumentException("null serverInfo");
    }
    if (owner == null) {
      throw new IllegalArgumentException("null owner");
    }
    if (computer == null) {
      throw new IllegalArgumentException("null computer");
    }
    myServerInfo = serverInfo;
    myOwnerName = owner;
    myComputer = computer;
    myTimestamp = Calendar.getInstance();
  }

  public WorkspaceInfo(final ServerInfo serverInfo,
                       final String name,
                       final String owner,
                       final String computer,
                       final String comment,
                       final Calendar timestamp) {
    this(serverInfo, owner, computer);
    if (name == null) {
      throw new IllegalArgumentException("null name");
    }

    myOriginalName = name;
    myComment = comment;
    myTimestamp = timestamp;
  }

  public ServerInfo getServer() {
    return myServerInfo;
  }

  public String getOwnerName() {
    return myOwnerName;
  }

  public String getComputer() {
    return myComputer;
  }

  public String getName() {
    return myModifiedName != null ? myModifiedName : myOriginalName;
  }

  public void setName(final String name) {
    myModifiedName = name;
  }

  public String getComment() {
    return myComment;
  }

  public void setComment(final String comment) {
    myComment = comment;
  }

  public Calendar getTimestamp() {
    return myTimestamp;
  }

  public void setTimestamp(final Calendar timestamp) {
    myTimestamp = timestamp;
  }

  //public boolean isWorkingFoldersCached() {
  //  return myWorkingFoldersCached;
  //}

  //public void setWorkingFoldersCached(final boolean workingFoldersCached) {
  //  myWorkingFoldersCached = workingFoldersCached;
  //}

  public List<WorkingFolderInfo> getWorkingFoldersInfos() {
    return Collections.unmodifiableList(myWorkingFoldersInfos);
  }

  public void loadFromServer() throws RemoteException {
    Workspace workspaceBean = getServer().getVCS().getWorkspace(getName(), getOwnerName());
    fromBean(workspaceBean, this);

    //setWorkingFoldersCached(false);
    Workstation.getInstance().updateCacheFile();
  }

  public void addWorkingFolderInfo(final WorkingFolderInfo workingFolderInfo) {
    myWorkingFoldersInfos.add(workingFolderInfo);
  }

  public void removeWorkingFolderInfo(final WorkingFolderInfo folderInfo) {
    myWorkingFoldersInfos.remove(folderInfo);
  }

  public void saveToServer() throws RemoteException {
    if (myOriginalName != null) {
      getServer().getVCS().updateWorkspace(myOriginalName, getOwnerName(), toBean(this));
    }
    else {
      // TODO: refactor
      getServer().getVCS().createWorkspace(toBean(this));
      getServer().addWorkspaceInfo(this);
    }
    myOriginalName = getName();
    Workstation.getInstance().updateCacheFile();
  }

  private static Workspace toBean(WorkspaceInfo info) {
    final ArrayOfWorkingFolder folders = new ArrayOfWorkingFolder();
    List<WorkingFolder> foldersList = new ArrayList<WorkingFolder>(info.getWorkingFoldersInfos().size());
    for (WorkingFolderInfo folderInfo : info.getWorkingFoldersInfos()) {
      foldersList.add(toBean(folderInfo));
    }
    folders.setWorkingFolder(foldersList.toArray(new WorkingFolder[foldersList.size()]));

    Workspace bean = new Workspace();
    bean.setComment(info.getComment());
    bean.setComputer(info.getComputer());
    bean.setFolders(folders);
    bean.setLastAccessDate(info.getTimestamp());
    bean.setName(info.getName());
    bean.setOwner(info.getOwnerName());
    return bean;
  }

  private static WorkingFolder toBean(final WorkingFolderInfo folderInfo) {
    WorkingFolder bean = new WorkingFolder();
    bean.setItem(folderInfo.getServerPath());
    bean.setLocal(folderInfo.getLocalPath());
    bean.setType(folderInfo.getStatus() == WorkingFolderInfo.Status.Cloaked ? WorkingFolderType.Cloak : WorkingFolderType.Map);
    return bean;
  }

  private static WorkingFolderInfo fromBean(WorkingFolder bean) {
    WorkingFolderInfo.Status status =
      WorkingFolderType.Cloak.equals(bean.getType()) ? WorkingFolderInfo.Status.Cloaked : WorkingFolderInfo.Status.Active;
    return new WorkingFolderInfo(status, bean.getLocal(), bean.getItem());
  }

  private static void fromBean(Workspace bean, WorkspaceInfo info) {
    info.setComment(bean.getComment());
    info.setTimestamp(bean.getLastAccessDate());
    final WorkingFolder[] folders;
    if (bean.getFolders().getWorkingFolder() != null) {
      folders = bean.getFolders().getWorkingFolder();
    }
    else {
      folders = new WorkingFolder[0];
    }
    List<WorkingFolderInfo> workingFoldersInfos = new ArrayList<WorkingFolderInfo>(folders.length);
    for (WorkingFolder folderBean : folders) {
      workingFoldersInfos.add(fromBean(folderBean));
    }
    info.myWorkingFoldersInfos = workingFoldersInfos;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "WorkspaceInfo[server=" +
           getServer().getUri() +
           ",name=" +
           getName() +
           ",owner=" +
           getOwnerName() +
           ",computer=" +
           getComputer() +
           ",comment=" +
           getComment() +
           "]";
  }

}
