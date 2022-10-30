/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50725
 Source Host           : localhost:3306
 Source Schema         : oauth2

 Target Server Type    : MySQL
 Target Server Version : 50725
 File Encoding         : 65001

 Date: 23/10/2022 22:01:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_users
-- ----------------------------
DROP TABLE IF EXISTS `t_users`;
CREATE TABLE `t_users`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `roles` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '角色',
  `is_valid` tinyint(1) NULL DEFAULT NULL,
  `create_date` datetime(0) NULL DEFAULT NULL,
  `update_date` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_users
-- ----------------------------
INSERT INTO `t_users` VALUES (1, 'zjq', '共饮一杯无', '17688888888', 'abc@zjq.com', 'e10adc3949ba59abbe56e057f20f883e', '/abc', 'ROLE_USER', 1, '2022-08-08 08:08:08', '2022-10-10 10:10:10');
INSERT INTO `t_users` VALUES (2, 'test', '测试用户', '13666666666', NULL, 'e10adc3949ba59abbe56e057f20f883e', '/test', 'ROLE_USER', 1, '2022-08-08 08:08:08', '2022-10-10 10:10:10');
INSERT INTO `t_users` VALUES (3, 'zhangsan', '张老三', '13666666688', NULL, 'e10adc3949ba59abbe56e057f20f883e', '/test2', 'ROLE_USER', 1, '2022-08-08 08:08:08', '2022-10-10 10:10:10');
INSERT INTO `t_users` VALUES (5, 'wangwu', '王老五', '18066669999', NULL, 'e10adc3949ba59abbe56e057f20f883e', '/aaa', 'ROLE_USER', 0, '2022-08-08 08:08:08', '2022-10-10 10:10:10');
INSERT INTO `t_users` VALUES (6, 'zhanjq', '共饮一杯无二号', '15388886666', NULL, 'e10adc3949ba59abbe56e057f20f883e', NULL, 'ROLE_USER', 1, '2022-10-29 22:48:45', '2022-10-29 22:48:45');

SET FOREIGN_KEY_CHECKS = 1;
