package com.tencent.qqgame.client.scene.model;



/**
 * 游戏数据模型
 * <p>
 *
 * </p>
 */
public class GameModel{


    /**
     * 用补丁包恢复
     * @param game
     */
    public static boolean restoreAPK(String oldfileDir, String newfileDir, String patchfileDir){
        int ret = -1;

        if(oldfileDir != null &&
                newfileDir != null && newfileDir.length() > 0 &&
                patchfileDir != null && patchfileDir.length() > 0){
            //#if ${polish.debug}
            System.out.println("[makeAPK] oldfile=" + oldfileDir);
            System.out.println("[makeAPK] newfile=" + newfileDir);
            System.out.println("[makeAPK] patchfile=" + patchfileDir);
            //#endif
            ret = gamePatch(oldfileDir, newfileDir, patchfileDir);
        }
        //#if ${polish.debug}
        System.out.println("[makeAPK] ret=" + ret);
        //#endif
        return ret==0;
    }

    public native static int gamePatch(String oldfile, String newfile, String patchfile);


}
