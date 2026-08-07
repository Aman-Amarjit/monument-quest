import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import monumentRoutes from './routes/monumentRoutes';

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

app.use('/api/monuments', monumentRoutes);

app.get('/health', (req, res) => {
  res.send('Monument Quest Backend is running!');
});

app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});
