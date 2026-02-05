import React, { memo, useState } from "react";
import { useTranslation } from "react-i18next";

import { Form, Modal, Upload, type UploadProps, message } from "antd";
import { InboxOutlined } from "@ant-design/icons";

import type { BranchStatistics } from "@/types/branchStatistics";

const { Dragger } = Upload;

import "@/i18n";
import type { RcFile } from "antd/es/upload/interface";

interface BranchDetailsProps {
    isOpen: boolean;
    setIsOpen: (open: boolean) => void;

    branchStats: BranchStatistics;
}

const props: UploadProps = {
    name: "file",
    multiple: true,
    onChange(info) {
        const { status } = info.file;
        if (status !== "uploading") {
            console.log(info.file, info.fileList);
        }
        if (status === "done") {
            message.success(`${info.file.name} file uploaded successfully.`);
        } else if (status === "error") {
            message.error(`${info.file.name} file upload failed.`);
        }
    },
    beforeUpload: (file: RcFile, fileList: RcFile[]) => {
        console.log("Before upload called", { file, fileList });
        return false;
    },
    onDrop(e) {
        console.log("Dropped files", e.dataTransfer.files);
    },
};

const ScreenshotUpload: React.FC<BranchDetailsProps> = ({
    isOpen,
    setIsOpen,
    branchStats,
}) => {
    const [confirmLoading, setConfirmLoading] = useState(false);
    const [modalText, setModalText] = useState("Content of the modal");

    const { t } = useTranslation("branch");

    const handleOk = () => {
        setModalText("The modal will be closed after two seconds");
        setConfirmLoading(true);
        setTimeout(() => {
            setIsOpen(false);
            setConfirmLoading(false);
        }, 2000);
    };

    const handleCancel = () => {
        console.log("Clicked cancel button");
        setIsOpen(false);
    };

    return (
        <Modal
            title={t("uploadScreenshot")}
            open={isOpen}
            onOk={handleOk}
            confirmLoading={confirmLoading}
            onCancel={handleCancel}
        >
            <section className="m-1">
                <Form>
                    <Dragger {...props}>
                        <p className="ant-upload-drag-icon">
                            <InboxOutlined />
                        </p>
                        <p className="ant-upload-text">
                            Click or drag file to this area to upload
                        </p>
                        <p className="ant-upload-hint">
                            Support for a single or bulk upload. Strictly
                            prohibited from uploading company data or other
                            banned files.
                        </p>
                    </Dragger>
                </Form>
            </section>
        </Modal>
    );
};

export default memo(ScreenshotUpload);
