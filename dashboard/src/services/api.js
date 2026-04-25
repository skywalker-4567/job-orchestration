import axios from 'axios';

const api = axios.create({ baseURL: '' });

export const getJobMetrics      = () => api.get('/metrics/jobs').then(r => r.data);
export const getExecutionMetrics = () => api.get('/metrics/executions').then(r => r.data);
export const getAllJobs          = () => api.get('/jobs').then(r => r.data);
export const getJob             = (jobId) => api.get(`/jobs/${jobId}`).then(r => r.data);
export const getExecutions      = (jobId) => api.get(`/jobs/${jobId}/executions`).then(r => r.data);