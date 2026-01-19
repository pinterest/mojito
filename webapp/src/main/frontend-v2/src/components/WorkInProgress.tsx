import { Card, Progress, Result, Space, theme, Typography } from "antd";
import { ToolOutlined } from "@ant-design/icons";

const { Title, Paragraph } = Typography;

export default function WorkInProgress() {
    const { token } = theme.useToken();

    return (
        <Card
            variant="outlined"
            style={{
                maxWidth: 600,
                margin: "auto",
                marginTop: "5vh",
                textAlign: "center",
            }}
        >
            <Result
                icon={<ToolOutlined style={{ color: token.colorPrimary }} />}
                title={<Title level={2}>Coming soon</Title>}
                subTitle={
                    <Space orientation="vertical" size="large">
                        <Paragraph>
                            The site is currently under construction. We're
                            working hard to bring you an incredible experience.
                        </Paragraph>

                        <Paragraph className="text-sm text-gray-500 mb-2">
                            Progress: Just started...
                        </Paragraph>

                        <Progress
                            percent={10}
                            status="active"
                            strokeColor={{
                                "0%": "#BCB382",
                                "100%": token.colorPrimary,
                            }}
                        />
                    </Space>
                }
            />
        </Card>
    );
}
